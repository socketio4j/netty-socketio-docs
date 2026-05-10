#!/usr/bin/env python3

import argparse
import csv
import json
import math
import os
from collections import defaultdict

from PIL import Image, ImageDraw, ImageFont


COLORS = ["#1f77b4", "#d62728", "#2ca02c", "#9467bd"]


def load_font(size, bold=False):
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/Library/Fonts/Arial.ttf",
    ]
    for path in candidates:
        if path and os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


TITLE = load_font(32, True)
H2 = load_font(19, True)
TEXT = load_font(16)
SMALL = load_font(12)


def mb(value):
    return float(value) / (1024 * 1024)


def fmt(value):
    return "{:,.0f}".format(value)


def fmt2(value):
    return "{:,.2f}".format(value)


def load_memory_series(result_json, result):
    result_dir = os.path.dirname(result_json)
    memory_csv = result.get("memoryCsv")
    if not memory_csv:
        raise ValueError("Result JSON does not contain memoryCsv")

    memory_csv_path = os.path.join(result_dir, memory_csv)
    rows = list(csv.DictReader(open(memory_csv_path, newline="")))
    if not rows:
        return {}

    first_timestamp = min(int(row["timestamp"]) for row in rows)
    series = defaultdict(list)
    for row in rows:
        heap = int(row["heapUsed"])
        rss = int(row.get("processRss") or 0)
        direct = int(row.get("directMemoryUsed") or 0)
        mapped = int(row.get("mappedMemoryUsed") or 0)
        series[row["node"]].append({
            "t": (int(row["timestamp"]) - first_timestamp) / 1000.0,
            "phase": row["phase"],
            "heap": mb(heap),
            "rss": mb(rss),
            "native": mb(max(0, rss - heap)),
            "direct": mb(direct + mapped),
        })
    return series


def draw_panel(draw, box, series, key, title):
    x0, y0, x1, y1 = box
    left, top, right, bottom = x0 + 72, y0 + 30, x1 - 25, y1 - 42
    points = [point for node_points in series.values() for point in node_points]
    max_t = max([point["t"] for point in points] or [1])
    max_y = max([point[key] for point in points] or [1])
    unit = 250 if key != "heap" else 100
    max_y = max(10, math.ceil(max_y / unit) * unit)

    draw.text((left, y0), title, fill="#222", font=H2)
    draw.rectangle([left, top, right, bottom], outline="#444", width=1)

    for i in range(1, 5):
        y = bottom - (bottom - top) * i / 4
        draw.line([left, y, right, y], fill="#e4e4e4")
        draw.text((x0 + 5, y - 7), "{:.0f} MB".format(max_y * i / 4), fill="#555", font=SMALL)

    for i in range(0, 7):
        x = left + (right - left) * i / 6
        draw.line([x, top, x, bottom], fill="#f0f0f0")
        if i in (0, 3, 6):
            draw.text((x - 15, bottom + 8), "{:.0f}m".format(max_t * i / 360), fill="#555", font=SMALL)

    for index, (node, node_points) in enumerate(sorted(series.items())):
        color = COLORS[index % len(COLORS)]
        xy = []
        for point in node_points:
            x = left + (right - left) * (point["t"] / max_t if max_t else 0)
            y = bottom - (bottom - top) * (point[key] / max_y if max_y else 0)
            xy.append((x, y))
            if point["phase"] == "post-gc":
                draw.ellipse([x - 4, y - 4, x + 4, y + 4], fill=color)
        if len(xy) > 1:
            draw.line(xy, fill=color, width=2)


def draw_summary(draw, result):
    draw.text((1080, 105), "Memory Summary", fill="#222", font=H2)
    for index, summary in enumerate(result.get("memorySummaries", [])):
        y = 138 + index * 105
        first_native = summary.get("firstProcessRss", 0) - summary.get("firstHeapUsed", 0)
        last_native = summary.get("lastProcessRss", 0) - summary.get("lastHeapUsed", 0)
        post_gc_native = summary.get("postGcProcessRss", 0) - summary.get("postGcHeapUsed", 0)
        direct_peak = summary.get("maxDirectMemoryUsed", 0) + summary.get("maxMappedMemoryUsed", 0)

        draw.text((1080, y),
                  "{}: samples {}, GC {} / {} ms".format(
                      summary["nodeName"], summary["samples"], summary["gcCount"], summary["gcTimeMs"]),
                  fill="#111", font=SMALL)
        draw.text((1080, y + 20),
                  "end RSS delta {:+.1f} MB, post-GC+60s RSS delta {:+.1f} MB".format(
                      mb(summary.get("rssLastMinusFirst", 0)), mb(summary.get("rssPostGcMinusFirst", 0))),
                  fill="#555", font=SMALL)
        draw.text((1080, y + 40),
                  "direct/mapped offheap delta {:+.1f} MB, peak {:.1f} MB".format(
                      mb(summary.get("directLastMinusFirst", 0)), mb(direct_peak)),
                  fill="#555", font=SMALL)
        draw.text((1080, y + 60),
                  "native approx end delta {:+.1f} MB, post-GC+60s delta {:+.1f} MB".format(
                      mb(last_native - first_native), mb(post_gc_native - first_native)),
                  fill="#555", font=SMALL)
        draw.text((1080, y + 80),
                  "heap peak {:.1f} MB, post-GC+60s heap delta {:+.1f} MB".format(
                      mb(summary.get("maxHeapUsed", 0)), mb(summary.get("postGcMinusFirst", 0))),
                  fill="#555", font=SMALL)


def default_output_path(result_json, result):
    result_dir = os.path.dirname(result_json)
    base_name = os.path.splitext(os.path.basename(result_json))[0]
    return os.path.join(result_dir, base_name + "-memory-chart.png")


def create_chart(result_json, output_path):
    result = json.load(open(result_json))
    series = load_memory_series(result_json, result)

    image = Image.new("RGB", (1700, 1260), "white")
    draw = ImageDraw.Draw(image)
    mode = result.get("mode", "standalone").capitalize()
    duration_minutes = max(1, round(result.get("testDuration", 0) / 60000.0))

    draw.text((35, 25),
              "Netty Socket.IO {} {:,} Clients, {} min Messaging".format(
                  mode, result.get("clientCount", 0), duration_minutes),
              fill="#111", font=TITLE)
    draw.text((35, 68), result.get("timestamp", ""), fill="#666", font=TEXT)

    metrics = [
        ("Clients", fmt(result.get("clientCount", 0))),
        ("Messages", "{} sent / {} received".format(fmt(result.get("messagesSent", 0)), fmt(result.get("messagesReceived", 0)))),
        ("Errors", fmt(result.get("errors", 0))),
        ("Loss Rate", "{:.8f}%".format(result.get("messageLossRate", 0) * 100)),
        ("Msg/sec", fmt2(result.get("messagesPerSecond", 0))),
        ("Avg Latency", "{:.2f} ms".format(result.get("avgLatency", 0))),
        ("P50 Latency", "{} ms".format(result.get("p50Latency", 0))),
        ("P90 Latency", "{} ms".format(result.get("p90Latency", 0))),
        ("P99 Latency", "{} ms".format(result.get("p99Latency", 0))),
        ("Duration", "{} ms".format(result.get("testDuration", 0))),
        ("Server Heap", result.get("serverJvmArgs", "")),
    ]
    for index, (key, value) in enumerate(metrics):
        column = 35 if index < 6 else 560
        y = 115 + (index % 6) * 31
        draw.text((column, y), key + ":", fill="#555", font=TEXT)
        draw.text((column + 140, y), value, fill="#111", font=TEXT)

    draw_summary(draw, result)
    draw.text((35, 310),
              "Note: direct/mapped = java.nio BufferPool. Native/off-heap approx = process RSS - heap used. "
              "Post-GC sample waits after explicit full GC.",
              fill="#666", font=SMALL)

    draw_panel(draw, (35, 345, 1665, 600), series, "heap", "Heap Used")
    draw_panel(draw, (35, 630, 1665, 885), series, "rss", "Process RSS")
    draw_panel(draw, (35, 915, 1665, 1170), series, "native", "Native / Off-Heap Approx (RSS - Heap)")

    legend_y = 1190
    for index, node in enumerate(sorted(series)):
        x = 85 + index * 180
        color = COLORS[index % len(COLORS)]
        draw.rectangle([x, legend_y, x + 16, legend_y + 16], fill=color)
        draw.text((x + 23, legend_y - 2), node, fill="#222", font=SMALL)

    image.save(output_path)
    print(output_path)


def main():
    parser = argparse.ArgumentParser(description="Generate memory chart for a smoke-test result JSON.")
    parser.add_argument("result_json")
    parser.add_argument("--output")
    args = parser.parse_args()

    output = args.output or default_output_path(args.result_json, json.load(open(args.result_json)))
    create_chart(args.result_json, output)


if __name__ == "__main__":
    main()

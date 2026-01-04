---
icon: thumbs-up
---

# Tips & Best Practices

* Avoid sending large objects; prefer IDs, deltas, or streamed binary data.
* Do not perform database access or other blocking operations inside event handlers.
* Keep event handlers lightweight—receive, validate, and emit only.
* Offload business logic to API or worker services, and emit only the results back to clients.
* Always validate incoming event payloads for auth, size limits and schema correctness.
* Use rooms to efficiently target messages to subsets of connected clients.
* Use namespaces to partition and isolate logical communication channels.
* Handle client disconnects carefully to prevent resource leaks and stale state.


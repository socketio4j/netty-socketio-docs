---
icon: comment-dots
layout:
  width: default
  title:
    visible: true
  description:
    visible: true
  tableOfContents:
    visible: true
  outline:
    visible: true
  pagination:
    visible: true
  metadata:
    visible: true
metaLinks:
  alternates:
    - https://app.gitbook.com/s/yE16Xb3IemPxJWydtPOj/getting-started/quickstart
---

# Simple chat app

## 1 to 1 Chat

### Server

```java
import com.socketio4j.socketio.*;

/**
 * Direct 1-to-1 Chat Server (single file)
 * Room name is deterministic: alphabetical pair
 * e.g. directRoom("bob","alice") = "alice:bob"
 */
public class DirectChatServer {

    public static void main(String[] args) {
        Configuration cfg = new Configuration();
        cfg.setHostname("localhost");
        cfg.setPort(9092);

        SocketIOServer server = new SocketIOServer(cfg);

        // client requests to join direct chat room
        server.addEventListener("chat:join", ChatRequest.class, (client, req, ack) -> {
            String room = RoomUtil.directRoom(req.me, req.with);
            client.joinRoom(room);
            System.out.println(req.me + " joined room " + room);
        });

        // send direct message to private room
        server.addEventListener("chat:msg", ChatMessage.class, (client, msg, ack) -> {
            String room = RoomUtil.directRoom(msg.from, msg.to);
            server.getRoomOperations(room).sendEvent("chat:msg", msg);
        });

        server.start();
        System.out.println("Direct chat server :9092 started");
    }

    // === Data Structures ===

    public static class ChatRequest {
        public String me;     // current user
        public String with;   // target user
    }

    public static class ChatMessage {
        public String from;   // sender
        public String to;     // receiver
        public String text;   // message
    }

    // === Utility Class ===

    public static class RoomUtil {
        /**
         * Deterministic private room name
         * ensures same room for both orders
         */
        public static String directRoom(String u1, String u2) {
            if (u1.compareToIgnoreCase(u2) < 0) return u1 + ":" + u2;
            return u2 + ":" + u1;
        }
    }
}

```

### Client

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Direct Chat App</title>
  <style>
    body { font-family: sans-serif; margin: 20px; }
    #messages { border:1px solid #ccc; padding:10px; height:250px; overflow-y:auto; }
  </style>
</head>

<body>
  <h2>💬 Direct Chat</h2>

  <label>Your name</label><br>
  <input id="me" placeholder="alice"><br><br>

  <label>Chat with</label><br>
  <input id="with" placeholder="bob"><br><br>

  <button onclick="joinChat()">Join Chat</button>

  <hr>

  <div id="status">Not connected</div>

  <div id="messages"></div><br>

  <input id="msg" placeholder="Type message..." style="width: 250px;">
  <button onclick="sendMsg()">Send</button>

  <!-- socket.io client -->
  <script src="https://cdn.socket.io/4.7.5/socket.io.min.js"></script>
  <script>
    let socket;
    let meUser;
    let withUser;

    function joinChat() {
      meUser = document.getElementById("me").value.trim();
      withUser = document.getElementById("with").value.trim();

      if (!meUser || !withUser) {
        alert("Enter both usernames");
        return;
      }

      socket = io("http://localhost:9092");

      socket.on("connect", () => {
        document.getElementById("status").innerText =
          `Connected as ${meUser}, chatting with ${withUser}`;

        // ask server to join direct room
        socket.emit("chat:join", { me: meUser, with: withUser });
      });

      // incoming messages
      socket.on("chat:msg", m => appendMsg(`${m.from}: ${m.text}`));

      socket.on("disconnect", () => {
        document.getElementById("status").innerText = "Disconnected";
      });
    }

    function sendMsg() {
      const text = document.getElementById("msg").value;
      if (!text || !socket) return;

      socket.emit("chat:msg", {
        from: meUser,
        to: withUser,
        text: text
      });

      appendMsg(`You: ${text}`);
      document.getElementById("msg").value = "";
    }

    function appendMsg(line) {
      const div = document.getElementById("messages");
      div.innerHTML += `<div>${line}</div>`;
      div.scrollTop = div.scrollHeight;
    }
  </script>
</body>
</html>

```

## Group Chat

### Server

```java
import com.socketio4j.socketio.*;

public class GroupChatServer {
    public static void main(String[] args) {
        Configuration cfg = new Configuration();
        cfg.setHostname("localhost");
        cfg.setPort(9092);

        SocketIOServer server = new SocketIOServer(cfg);

        // join a room
        server.addEventListener("group:join", JoinRequest.class, (client, req, ack) -> {
            client.joinRoom(req.room);
            System.out.println(req.user + " joined room: " + req.room);
        });

        // send message to room
        server.addEventListener("group:msg", GroupMessage.class, (client, msg, ack) -> {
            server.getRoomOperations(msg.room).sendEvent("group:msg", msg);
        });

        server.start();
        System.out.println("Group Chat server running at :9092");
    }

    public static class JoinRequest {
        public String user;
        public String room;
    }

    public static class GroupMessage {
        public String user;
        public String room;
        public String text;
    }
}

```

### Client

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Group Chat</title>
  <style>
    body { font-family: sans-serif; padding: 20px; }
    #messages { border:1px solid #ccc; padding:10px; height:260px; overflow-y:auto; }
  </style>
</head>

<body>

  <h2>💬 Group Chat</h2>

  <label>Your name</label><br>
  <input id="user" placeholder="alice"><br><br>

  <label>Room</label><br>
  <input id="room" placeholder="sports"><br><br>

  <button onclick="joinRoom()">Join Room</button>

  <hr>
  <div id="status">Not connected</div>

  <div id="messages"></div><br>

  <input id="msg" placeholder="Type message..." style="width: 260px;">
  <button onclick="sendMsg()">Send</button>

  <!-- socket.io client -->
  <script src="https://cdn.socket.io/4.7.5/socket.io.min.js"></script>

  <script>
    let socket;
    let username;
    let roomname;

    function joinRoom() {
      username = document.getElementById("user").value.trim();
      roomname = document.getElementById("room").value.trim();

      if (!username || !roomname) {
        alert("enter user and room");
        return;
      }

      socket = io("http://localhost:9092");

      socket.on("connect", () => {
        document.getElementById("status").innerText =
          `Connected as "${username}" in room "${roomname}"`;

        socket.emit("group:join", { user: username, room: roomname });
      });

      socket.on("group:msg", msg => {
        appendMsg(`${msg.user}: ${msg.text}`);
      });

      socket.on("disconnect", () => {
        document.getElementById("status").innerText = "Disconnected";
      });
    }

    function sendMsg() {
      const text = document.getElementById("msg").value.trim();
      if (!text || !socket) return;

      socket.emit("group:msg", {
        user: username,
        room: roomname,
        text: text
      });

      appendMsg(`You: ${text}`);
      document.getElementById("msg").value = "";
    }

    function appendMsg(line) {
      const box = document.getElementById("messages");
      box.innerHTML += `<div>${line}</div>`;
      box.scrollTop = box.scrollHeight;
    }
  </script>

</body>
</html>

```

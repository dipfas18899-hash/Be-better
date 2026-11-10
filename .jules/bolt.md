
## 2024-05-18 - Optimized Broadcast JSON Serialization
**Learning:** In Python's FastAPI backend for the party server, broadcasting a message to multiple WebSockets individually serializes the identical dictionary payload into JSON for each socket using `send_json`. When broadcasting queue updates or play state across 5 devices concurrently, this causes redundant serialization overhead for each device.
**Action:** Serialize the payload to a JSON string once before the broadcast loop and send it using `send_text` (or equivalent method) to all targets, reducing CPU time spent on duplicate json serialization on hot paths.

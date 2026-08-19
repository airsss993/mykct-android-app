"""Заглушка портала + auth-сервиса: даёт college-app-core данные без внутренней сети колледжа."""
import json, os
from datetime import date, timedelta
from http.server import BaseHTTPRequestHandler, HTTPServer

HERE = os.path.dirname(os.path.abspath(__file__))

def mode():
    try:
        return open(os.path.join(HERE, "mode")).read().strip()
    except OSError:
        return "normal"

SCHEDULE = [
    {"ClID": "12345", "Day": "2026-09-01", "start": "2026-09-01 09:00", "end": "2026-09-01 10:30",
     "title": "Математика", "topic": "Пределы", "room": "301", "group": "ИТ25-11",
     "color": "#4F8DF7", "type": "lecture"},
    {"ClID": "12346", "Day": "2026-09-01", "start": "9:00", "end": "10:30",
     "title": "Английский", "topic": "", "room": "—", "group": "ИТ25-11", "color": "#F79E4F",
     "SubGroup": [
        {"SClID": "1", "SGrID": "A0.11", "SGCaID": "210", "STopic": "Present Simple", "STitle": "Английский A0.11"},
        {"SClID": "2", "SGrID": "B1.11", "SGCaID": "211", "STopic": "Essays", "STitle": "Английский B1.11"}]},
    {"ClID": "12347", "Day": "2026-09-02", "start": "10:40", "end": "12:10",
     "title": "Программирование", "topic": "", "room": "", "group": "ИТ25-11", "color": "#7F4FF7",
     "SubGroup": [
        {"SClID": "3", "SGrID": "Подгр1", "SGCaID": "305", "STopic": "Kotlin", "STitle": "Программирование п1"},
        {"SClID": "4", "SGrID": "Подгр2", "SGCaID": "306", "STopic": "Go", "STitle": "Программирование п2"}]},
]

ATTENDANCE = [
    {"ClID": 12345, "Day": "2026-09-01", "start": "2026-09-01 09:00", "end": "2026-09-01 10:30",
     "topic": "Пределы", "title": "Математика", "room": "301", "color": "#4F8DF7", "status": 2},
    {"ClID": 12346, "Day": "2026-09-01", "start": "9:00", "end": "10:30",
     "topic": "", "title": "Английский", "room": "—", "color": "#F79E4F", "status": 0,
     "SubGroup": [{"SClID": 1, "SCaID": "210", "STopic": "Present Simple", "STitle": "Английский A0.11"}]},
    {"ClID": 12347, "Day": "2026-09-02", "start": "10:40", "end": "12:10",
     "topic": "Kotlin", "title": "Программирование", "room": "305", "color": "#7F4FF7", "status": 1},
]

SUBJECTS = [{"SuIDcrc": "crc1", "SuID": "su-1", "Title": "Математика"},
            {"SuIDcrc": "crc2", "SuID": "su-2", "Title": "Программирование"}]

SCORES = {"Математика": {"Пределы": [{"DateF": "2026-09-01", "DateP": "2026-09-01",
                                       "Score": "5", "MaxScore": 5, "Description": "Работа на паре"},
                                      {"DateF": "2026-09-08", "DateP": "", "Score": "",
                                       "MaxScore": 5, "Description": "Не сдано"}]}}

USER = {"id": "student01", "username": "student01", "role": "student",
        "academic_group": "ИТ25-11", "profile": "", "subgroup": "Подгр1", "english_group": "A0.11"}

def shift(items, d_start):
    """Портал отдаёт то, что попало в запрошенный период — двигаем даты под d_start."""
    if not d_start:
        return items
    base = date.fromisoformat(d_start)
    out = []
    for i, it in enumerate(items):
        it = json.loads(json.dumps(it))
        day = (base + timedelta(days=0 if i < 2 else 1)).isoformat()
        it["Day"] = day
        for k in ("start", "end"):
            if " " in it[k]:
                it[k] = day + " " + it[k].split(" ")[-1]
        out.append(it)
    return out


def payload(path, req):
    m = mode()
    if path.endswith("schedule25.php"):
        return None if m == "null" else ([] if m == "empty" else shift(SCHEDULE, req.get("d_start")))
    if path.endswith("classdetails25.php"):
        return {"ClID": "12345", "teacher": "Иванов И.И.", "any": {"nested": True}}
    if path.endswith("/attendance"):
        return None if m == "null" else ([] if m == "empty" else shift(ATTENDANCE, req.get("d_start")))
    if path.endswith("/subjects"):
        return None if m == "null" else ([] if m == "empty" else SUBJECTS)
    if path.endswith("/score"):
        return [] if m in ("empty", "null") else SCORES
    if path.endswith("/app/signin"):
        return {"access_token": "acc-1", "refresh_token": "ref-1",
                "access_expires_in": 3600, "refresh_expires_in": 2592000, "user": USER}
    if path.endswith("/app/access"):
        return {"access_token": "acc-1", "expires_in": 3600, "user": USER}
    if path.endswith("/app/refresh"):
        return {"refresh_token": "ref-1", "expires_in": 2592000}
    if path.endswith("/app/signout"):
        return {"message": "successfully signed out"}
    if path.endswith("/validate"):
        return {"valid": True, "user": {"id": "student01", "username": "student01", "role": "student"}}
    return {"error": "unknown stub path"}

class H(BaseHTTPRequestHandler):
    def respond(self):
        n = int(self.headers.get("Content-Length") or 0)
        req = json.loads(self.rfile.read(n) or "{}") if n else {}
        data = json.dumps(payload(self.path, req if isinstance(req, dict) else {}), ensure_ascii=False).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    do_GET = do_POST = respond

    def log_message(self, *a):
        pass

HTTPServer(("127.0.0.1", 9911), H).serve_forever()

from flask import Flask, jsonify, request

app = Flask(__name__)

# Tareas almacenadas en memoria (lista de diccionarios)
tareas = [
    {"id": 1, "titulo": "Estudiar Flask", "completada": False},
    {"id": 2, "titulo": "Probar API REST", "completada": False}
]

# Usuario ficticio (semana 9 - consumido por la APK ContadorMG)
usuario = {
    "nombre": "Martín Gacitúa",
    "correo": "m.gacitua03@ufromail.cl",
    "rut": "11.111.111-1"
}

# Credenciales válidas para el login
credenciales = {
    "login": "martin",
    "password": "gym2026"
}

# GET
@app.route("/todos", methods=["GET"])
def obtener_todos():
    return jsonify(tareas), 200

# POST
@app.route("/todos", methods=["POST"])
def agregar_todo():
    datos = request.get_json()

    if not datos or "titulo" not in datos:
        return jsonify({"error": "Falta el campo 'titulo'"}), 400

    nuevo_id = max([t["id"] for t in tareas], default=0) + 1
    nueva_tarea = {
        "id": nuevo_id,
        "titulo": datos["titulo"],
        "completada": datos.get("completada", False)
    }
    tareas.append(nueva_tarea)

    return jsonify(nueva_tarea), 201

# PUT
@app.route("/todos/<int:id>", methods=["PUT"])
def actualizar_todo(id):
    datos = request.get_json()

    for t in tareas:
        if t["id"] == id:
            t["titulo"] = datos.get("titulo", t["titulo"])
            t["completada"] = datos.get("completada", t["completada"])
            return jsonify(t), 200

    return jsonify({"error": "Tarea no encontrada"}), 404

# DELETE
@app.route("/todos/<int:id>", methods=["DELETE"])
def eliminar_todo(id):
    for indice, t in enumerate(tareas):
        if t["id"] == id:
            eliminado = tareas.pop(indice)
            return jsonify(eliminado), 200

    return jsonify({"error": "Tarea no encontrada"}), 404

# GET usuario: la APK pide los datos del usuario
@app.route("/usuario", methods=["GET"])
def obtener_usuario():
    return jsonify(usuario), 200

# POST login: la APK envía {"login": "...", "password": "..."}
@app.route("/login", methods=["POST"])
def login():
    datos = request.get_json(silent=True)

    if not datos or "login" not in datos or "password" not in datos:
        return jsonify({"valido": False, "mensaje": "Faltan login o password"}), 400

    if datos["login"] == credenciales["login"] and datos["password"] == credenciales["password"]:
        return jsonify({"valido": True, "mensaje": "Bienvenido", "usuario": usuario}), 200

    return jsonify({"valido": False, "mensaje": "Credenciales inválidas"}), 401


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000, debug=True)

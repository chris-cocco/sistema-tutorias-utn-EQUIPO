from flask import Flask, render_template, request, redirect, url_for, session, flash, send_file, g
from flask_sqlalchemy import SQLAlchemy
from werkzeug.security import generate_password_hash, check_password_hash
from datetime import datetime, timedelta
from fpdf import FPDF
from functools import wraps
import jwt
import shutil, os, threading, time
import logging
from logging.handlers import RotatingFileHandler

app = Flask(__name__)
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'clave_segura_sistema_tutorias_2026_utn')
CARPETA_BASE = os.path.dirname(__file__)
RUTA_DB = os.path.join(CARPETA_BASE, "sistema_tutorias.db")
app.config['SQLALCHEMY_DATABASE_URI'] = f'sqlite:///{RUTA_DB}'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['PERMANENT_SESSION_LIFETIME'] = timedelta(minutes=30)

db = SQLAlchemy(app)

# ===================== LOGS =====================
CARPETA_LOGS = os.path.join(CARPETA_BASE, "logs")
os.makedirs(CARPETA_LOGS, exist_ok=True)
manejador_logs = RotatingFileHandler(os.path.join(CARPETA_LOGS, "sistema.log"), maxBytes=1_000_000, backupCount=3, encoding="utf-8")
manejador_logs.setFormatter(logging.Formatter('%(asctime)s [%(levelname)s] %(message)s'))
logger = logging.getLogger('sistema_tutorias')
logger.setLevel(logging.INFO)
logger.addHandler(manejador_logs)

# ===================== MODELOS =====================
class Usuario(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    tipo = db.Column(db.String(20), nullable=False)
    credencial = db.Column(db.String(20), unique=True, nullable=False)
    contrasena = db.Column(db.String(250), nullable=False)
    nombre_completo = db.Column(db.String(100), nullable=False)
    intentos_fallidos = db.Column(db.Integer, default=0)
    bloqueado = db.Column(db.Boolean, default=False)

class Alumno(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    usuario_id = db.Column(db.Integer, db.ForeignKey('usuario.id'), unique=True, nullable=False)
    id_tutor = db.Column(db.Integer, db.ForeignKey('usuario.id'))
    rendimiento = db.Column(db.String(200), default="Sin registro")
    usuario = db.relationship('Usuario', foreign_keys=[usuario_id], backref=db.backref('perfil_alumno', uselist=False), single_parent=True)
    tutor = db.relationship('Usuario', foreign_keys=[id_tutor], backref='alumnos_asignados')

class Tutor(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    usuario_id = db.Column(db.Integer, db.ForeignKey('usuario.id'), unique=True, nullable=False)
    horario = db.Column(db.String(200), default="Lunes a Viernes 08:00 - 16:00")
    usuario = db.relationship('Usuario', foreign_keys=[usuario_id], backref=db.backref('perfil_tutor', uselist=False), single_parent=True)

class Tutoria(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    id_alumno = db.Column(db.Integer, db.ForeignKey('alumno.id'), nullable=False)
    id_tutor = db.Column(db.Integer, db.ForeignKey('usuario.id'), nullable=False)
    fecha = db.Column(db.DateTime, nullable=False)
    tema = db.Column(db.String(150), nullable=False)
    estado = db.Column(db.String(20), default="Solicitada")
    observaciones = db.Column(db.Text, default="")
    alumno = db.relationship('Alumno', backref='lista_tutorias')

class ConfiguracionRespaldos(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    activo = db.Column(db.Boolean, default=False)
    intervalo_horas = db.Column(db.Integer, default=24)
    ultima_ejecucion = db.Column(db.DateTime)

class Auditoria(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    accion = db.Column(db.String(100), nullable=False)
    fecha = db.Column(db.DateTime, default=datetime.utcnow)
    ip = db.Column(db.String(50), nullable=False)
    usuario = db.Column(db.String(100))

# ===================== DATOS INICIALES =====================
with app.app_context():
    db.create_all()
    if not ConfiguracionRespaldos.query.first():
        db.session.add(ConfiguracionRespaldos())
    if not Usuario.query.filter_by(credencial="coordinador").first():
        admin = Usuario(tipo="coordinador", credencial="coordinador", nombre_completo="Coordinador General", contrasena=generate_password_hash("clave_coordinador"))
        db.session.add(admin)
        db.session.flush()
    for n in range(1, 4):
        cred = f"TUT-{n:06d}"
        if not Usuario.query.filter_by(credencial=cred).first():
            usr = Usuario(tipo="tutor", credencial=cred, nombre_completo=f"Tutor {n}", contrasena=generate_password_hash(cred))
            db.session.add(usr)
            db.session.flush()
            db.session.add(Tutor(usuario_id=usr.id))
    for n in range(1, 11):
        cred = f"TIC-{n:06d}"
        if not Usuario.query.filter_by(credencial=cred).first():
            usr = Usuario(tipo="alumno", credencial=cred, nombre_completo=f"Alumno {n}", contrasena=generate_password_hash(cred))
            db.session.add(usr)
            db.session.flush()
            db.session.add(Alumno(usuario_id=usr.id, id_tutor=2, rendimiento="Promedio: 8.5"))
    db.session.commit()

# ===================== RESPALDOS =====================
CARPETA_RESPALDOS = os.path.join(CARPETA_BASE, "respaldos")
os.makedirs(CARPETA_RESPALDOS, exist_ok=True)

def tarea_respaldo_automatico():
    while True:
        with app.app_context():
            cfg = ConfiguracionRespaldos.query.first()
            if cfg and cfg.activo:
                ahora = datetime.utcnow()
                if not cfg.ultima_ejecucion or (ahora - cfg.ultima_ejecucion).total_seconds() >= cfg.intervalo_horas * 3600:
                    nombre = f"respaldo_{ahora.strftime('%Y%m%d_%H%M%S')}.db"
                    ruta_destino = os.path.join(CARPETA_RESPALDOS, nombre)
                    if os.path.exists(RUTA_DB):
                        shutil.copy2(RUTA_DB, ruta_destino)
                        cfg.ultima_ejecucion = ahora
                        db.session.commit()
        time.sleep(3600)

hilo = threading.Thread(target=tarea_respaldo_automatico, daemon=True)
hilo.start()

# ===================== FUNCIONES AUXILIARES =====================
def generar_pdf(datos, titulo, columnas):
    pdf = FPDF()
    pdf.add_page()
    pdf.set_font("Arial", "B", 16)
    pdf.cell(0, 10, titulo, ln=True, align="C")
    pdf.ln(5)
    pdf.set_font("Arial", "B", 10)
    anchos = [40, 50, 40, 50]
    for i, col in enumerate(columnas): pdf.cell(anchos[i], 8, col, border=1, align="C")
    pdf.ln()
    pdf.set_font("Arial", "", 9)
    for fila in datos:
        for i, celda in enumerate(fila): pdf.cell(anchos[i], 8, str(celda), border=1, align="C")
        pdf.ln()
    ruta = os.path.join(CARPETA_BASE, f"reporte_{datetime.now().strftime('%Y%m%d%H%M%S')}.pdf")
    pdf.output(ruta)
    return ruta

# ===================== AUTENTICACIÓN POR TOKEN (JWT) =====================
JWT_ALGORITMO = 'HS256'
JWT_MINUTOS_EXPIRACION = 30

def generar_token(usuario):
    payload = {
        'uid': usuario.id,
        'rol': usuario.tipo,
        'nombre': usuario.nombre_completo,
        'exp': datetime.utcnow() + timedelta(minutes=JWT_MINUTOS_EXPIRACION)
    }
    return jwt.encode(payload, app.config['SECRET_KEY'], algorithm=JWT_ALGORITMO)

def obtener_token_peticion():
    token = request.cookies.get('token')
    if token:
        return token
    encabezado = request.headers.get('Authorization', '')
    if encabezado.startswith('Bearer '):
        return encabezado.split(' ', 1)[1]
    return None

def requiere_rol(*roles_permitidos):
    def decorador(vista):
        @wraps(vista)
        def envoltura(*args, **kwargs):
            token = obtener_token_peticion()
            if not token:
                return redirect(url_for('login'))
            try:
                payload = jwt.decode(token, app.config['SECRET_KEY'], algorithms=[JWT_ALGORITMO])
            except jwt.ExpiredSignatureError:
                logger.warning(f"Token expirado al acceder a {request.path} desde {request.remote_addr}")
                flash("Tu sesión ha expirado, inicia sesión nuevamente", "error")
                return redirect(url_for('login'))
            except jwt.InvalidTokenError:
                logger.warning(f"Token inválido al acceder a {request.path} desde {request.remote_addr}")
                flash("Sesión inválida", "error")
                return redirect(url_for('login'))
            if roles_permitidos and payload['rol'] not in roles_permitidos:
                logger.warning(f"Acceso denegado: rol '{payload['rol']}' intentó entrar a {request.path} desde {request.remote_addr}")
                return redirect(url_for('login'))
            g.uid = payload['uid']
            g.rol = payload['rol']
            g.nombre = payload['nombre']
            return vista(*args, **kwargs)
        return envoltura
    return decorador

def requiere_rol_api(*roles_permitidos):
    """Igual que requiere_rol, pero para la API: responde JSON en vez de redirigir al login."""
    def decorador(vista):
        @wraps(vista)
        def envoltura(*args, **kwargs):
            token = obtener_token_peticion()
            if not token:
                return {'error': 'No se proporcionó un token de acceso'}, 401
            try:
                payload = jwt.decode(token, app.config['SECRET_KEY'], algorithms=[JWT_ALGORITMO])
            except jwt.ExpiredSignatureError:
                logger.warning(f"[API] Token expirado al acceder a {request.path} desde {request.remote_addr}")
                return {'error': 'El token ha expirado, inicia sesión nuevamente'}, 401
            except jwt.InvalidTokenError:
                logger.warning(f"[API] Token inválido al acceder a {request.path} desde {request.remote_addr}")
                return {'error': 'Token inválido'}, 401
            if roles_permitidos and payload['rol'] not in roles_permitidos:
                logger.warning(f"[API] Acceso denegado: rol '{payload['rol']}' intentó entrar a {request.path} desde {request.remote_addr}")
                return {'error': 'No tienes permiso para acceder a este recurso'}, 403
            g.uid = payload['uid']
            g.rol = payload['rol']
            g.nombre = payload['nombre']
            return vista(*args, **kwargs)
        return envoltura
    return decorador

# ===================== LOGIN =====================
@app.route('/', methods=['GET','POST'])
def login():
    ip = request.remote_addr
    if request.method == 'POST':
        cred = request.form['credencial'].strip()
        passw = request.form['contrasena'].strip()
        usuario = Usuario.query.filter_by(credencial=cred).first()
        if not usuario or not check_password_hash(usuario.contrasena, passw):
            logger.warning(f"Login fallido para credencial '{cred}' desde {ip}")
            flash("Credenciales incorrectas", "error")
            return redirect(url_for('login'))
        if usuario.bloqueado:
            logger.warning(f"Intento de acceso de usuario bloqueado '{cred}' desde {ip}")
            flash("Usuario bloqueado", "error")
            return redirect(url_for('login'))
        usuario.intentos_fallidos = 0
        db.session.commit()
        db.session.add(Auditoria(accion=f"INGRESO: {usuario.tipo}", ip=ip, usuario=usuario.nombre_completo))
        db.session.commit()
        logger.info(f"Login exitoso: {usuario.tipo} '{usuario.credencial}' desde {ip}")
        flash(f"Bienvenido {usuario.nombre_completo}", "success")
        token = generar_token(usuario)
        respuesta = redirect(url_for(f"panel_{usuario.tipo}"))
        respuesta.set_cookie('token', token, httponly=True, samesite='Lax',
                              secure=request.is_secure, max_age=JWT_MINUTOS_EXPIRACION * 60)
        return respuesta
    return render_template('login.html')

@app.route('/salir')
def salir():
    respuesta = redirect(url_for('login'))
    respuesta.delete_cookie('token')
    return respuesta

# ===================== ALUMNO =====================
@app.route('/panel-alumno')
@requiere_rol('alumno')
def panel_alumno():
    alumno = Alumno.query.filter_by(usuario_id=g.uid).first()
    tutorias = Tutoria.query.filter_by(id_alumno=alumno.id).order_by(Tutoria.fecha.desc()).all()
    return render_template('alumno.html', alumno=alumno, tutorias=tutorias)

@app.route('/solicitar-tutoria', methods=['POST'])
@requiere_rol('alumno')
def solicitar_tutoria():
    alumno = Alumno.query.filter_by(usuario_id=g.uid).first()
    fecha = datetime.strptime(request.form['fecha'], '%Y-%m-%d')
    tema = request.form['tema'].strip()
    if not tema: flash("El tema no puede estar vacío", "error"); return redirect(url_for('panel_alumno'))
    nueva = Tutoria(id_alumno=alumno.id, id_tutor=alumno.id_tutor, fecha=fecha, tema=tema, estado="Solicitada")
    db.session.add(nueva)
    db.session.commit()
    flash("Solicitud enviada al tutor", "success")
    return redirect(url_for('panel_alumno'))

@app.route('/reporte-alumno-pdf')
@requiere_rol('alumno')
def reporte_alumno_pdf():
    alumno = Alumno.query.filter_by(usuario_id=g.uid).first()
    tutorias = Tutoria.query.filter_by(id_alumno=alumno.id).all()
    datos = [(t.fecha.strftime('%d/%m/%Y'), t.tema, t.estado, t.observaciones[:30]) for t in tutorias]
    ruta = generar_pdf(datos, f"Mis Tutorías - {alumno.usuario.nombre_completo}", ["Fecha", "Tema", "Estado", "Observaciones"])
    return send_file(ruta, as_attachment=True, download_name="mis_tutorias.pdf")

# ==== ✅ AQUÍ VAN LOS REPORTES PARA ALUMNO ====
@app.route('/reportes-alumno')
@requiere_rol('alumno')
def reportes_alumno():
    alumno = Alumno.query.filter_by(usuario_id=g.uid).first()
    mis_tutorias = Tutoria.query.filter_by(id_alumno=alumno.id).all()
    total = len(mis_tutorias)
    realizadas = sum(1 for t in mis_tutorias if t.estado == "Realizada")
    pendientes = sum(1 for t in mis_tutorias if t.estado in ["Solicitada", "Confirmada", "Asignada por tutor"])

    return render_template('reportes_alumno.html',
        total=total, realizadas=realizadas, pendientes=pendientes, tutorias=mis_tutorias)

# ===================== TUTOR =====================
@app.route('/panel-tutor')
@requiere_rol('tutor')
def panel_tutor():
    tutor = Tutor.query.filter_by(usuario_id=g.uid).first()
    tutorias = Tutoria.query.filter_by(id_tutor=tutor.usuario_id).order_by(Tutoria.fecha.desc()).all()
    alumnos = Alumno.query.filter_by(id_tutor=tutor.usuario_id).all()
    return render_template('tutor.html', tutor=tutor, alumnos=alumnos, tutorias=tutorias)

@app.route('/tutor/aceptar/<int:id>')
@requiere_rol('tutor')
def aceptar_tutoria(id):
    tut = Tutoria.query.get_or_404(id)
    tut.estado = "Confirmada"
    db.session.commit()
    flash("Tutoría aceptada correctamente", "success")
    return redirect(url_for('panel_tutor'))

@app.route('/tutor/editar-tutoria/<int:id>', methods=['GET'])
@requiere_rol('tutor')
def form_editar_tutoria(id):
    tut = Tutoria.query.get_or_404(id)
    return render_template('editar_tutoria.html', tutoria=tut)

@app.route('/tutor/editar-tutoria/<int:id>', methods=['POST'])
@requiere_rol('tutor')
def editar_tutoria(id):
    tut = Tutoria.query.get_or_404(id)
    tut.fecha = datetime.strptime(request.form['fecha'], '%Y-%m-%d')
    tut.tema = request.form['tema']
    tut.estado = request.form['estado']
    tut.observaciones = request.form['observaciones']
    db.session.commit()
    flash("Tutoría actualizada", "success")
    return redirect(url_for('panel_tutor'))

@app.route('/tutor/actualizar-horario', methods=['POST'])
@requiere_rol('tutor')
def actualizar_horario():
    tutor = Tutor.query.filter_by(usuario_id=g.uid).first()
    tutor.horario = request.form['horario']
    db.session.commit()
    flash("Horario actualizado", "success")
    return redirect(url_for('panel_tutor'))

@app.route('/tutor/crear-tutoria', methods=['POST'])
@requiere_rol('tutor')
def crear_tutoria():
    tutor = Tutor.query.filter_by(usuario_id=g.uid).first()
    fecha = datetime.strptime(request.form['fecha'], '%Y-%m-%d')
    nueva = Tutoria(id_alumno=request.form['id_alumno'], id_tutor=tutor.usuario_id, fecha=fecha, tema=request.form['tema'], estado="Asignada por tutor")
    db.session.add(nueva)
    db.session.commit()
    flash("Tutoría creada", "success")
    return redirect(url_for('panel_tutor'))

@app.route('/tutor/completar/<int:id>')
@requiere_rol('tutor')
def completar_tutoria(id):
    Tutoria.query.get_or_404(id).estado = "Realizada"
    db.session.commit()
    flash("Tutoría marcada como realizada", "success")
    return redirect(url_for('panel_tutor'))

@app.route('/reporte-tutor-pdf')
@requiere_rol('tutor')
def reporte_tutor_pdf():
    tutor = Tutor.query.filter_by(usuario_id=g.uid).first()
    tutorias = Tutoria.query.filter_by(id_tutor=tutor.usuario_id).all()
    datos = [(t.alumno.usuario.nombre_completo, t.fecha.strftime('%d/%m/%Y'), t.tema, t.estado) for t in tutorias]
    ruta = generar_pdf(datos, f"Tutorías a mi cargo - {tutor.usuario.nombre_completo}", ["Alumno", "Fecha", "Tema", "Estado"])
    return send_file(ruta, as_attachment=True, download_name="tutorias_tutor.pdf")

# ==== ✅ AQUÍ VAN LOS REPORTES PARA TUTOR ====
@app.route('/reportes-tutor')
@requiere_rol('tutor')
def reportes_tutor():
    tutor = Tutor.query.filter_by(usuario_id=g.uid).first()
    mis_tutorias = Tutoria.query.filter_by(id_tutor=tutor.usuario_id).all()
    mis_alumnos = Alumno.query.filter_by(id_tutor=tutor.usuario_id).count()
    total = len(mis_tutorias)
    realizadas = sum(1 for t in mis_tutorias if t.estado == "Realizada")
    pendientes = sum(1 for t in mis_tutorias if t.estado in ["Solicitada", "Confirmada", "Asignada por tutor"])

    return render_template('reportes_tutor.html',
        total=total, realizadas=realizadas, pendientes=pendientes, alumnos=mis_alumnos, tutorias=mis_tutorias)

# ===================== COORDINADOR =====================
@app.route('/panel-coordinador')
@requiere_rol('coordinador')
def panel_coordinador():
    return render_template('coordinador.html',
        usuarios=Usuario.query.all(), tutorias=Tutoria.query.all(),
        auditoria=Auditoria.query.order_by(Auditoria.fecha.desc()).limit(30).all(),
        respaldos=os.listdir(CARPETA_RESPALDOS), cfg=ConfiguracionRespaldos.query.first())

@app.route('/reportes')
@requiere_rol('coordinador')
def reportes():
    # === MÓDULO 1: GESTIÓN DE TUTORÍAS ===
    total_tutorias = Tutoria.query.count()
    solicitadas = Tutoria.query.filter_by(estado="Solicitada").count()
    confirmadas = Tutoria.query.filter_by(estado="Confirmada").count()
    realizadas = Tutoria.query.filter_by(estado="Realizada").count()
    asignadas = Tutoria.query.filter_by(estado="Asignada por tutor").count()
    # === MÓDULO 2: GESTIÓN DE USUARIOS ===
    total_alumnos = Usuario.query.filter_by(tipo="alumno").count()
    total_tutores = Usuario.query.filter_by(tipo="tutor").count()
    total_coordinadores = Usuario.query.filter_by(tipo="coordinador").count()
    activos = Usuario.query.filter_by(bloqueado=False).count()
    bloqueados = Usuario.query.filter_by(bloqueado=True).count()

    return render_template('reportes.html',
        total_tutorias=total_tutorias, solicitadas=solicitadas, confirmadas=confirmadas,
        realizadas=realizadas, asignadas=asignadas,
        total_alumnos=total_alumnos, total_tutores=total_tutores,
        total_coordinadores=total_coordinadores, activos=activos, bloqueados=bloqueados)

@app.route('/coordinador/crear-usuario', methods=['POST'])
@requiere_rol('coordinador')
def crear_usuario():
    tipo = request.form['tipo']; cred = request.form['credencial']; nombre = request.form['nombre']; clave = request.form['contrasena']
    if Usuario.query.filter_by(credencial=cred).first(): flash("Credencial ya existe", "error"); return redirect(url_for('panel_coordinador'))
    nuevo = Usuario(tipo=tipo, credencial=cred, nombre_completo=nombre, contrasena=generate_password_hash(clave))
    db.session.add(nuevo); db.session.flush()
    if tipo == "alumno": db.session.add(Alumno(usuario_id=nuevo.id, id_tutor=None))
    if tipo == "tutor": db.session.add(Tutor(usuario_id=nuevo.id))
    db.session.add(Auditoria(accion=f"CREÓ USUARIO: {cred}", ip=request.remote_addr, usuario=g.nombre))
    db.session.commit()
    flash("Usuario creado correctamente", "success")
    return redirect(url_for('panel_coordinador'))

@app.route('/coordinador/asignar-tutor/<int:id_alumno>', methods=['POST'])
@requiere_rol('coordinador')
def asignar_tutor(id_alumno):
    Alumno.query.get_or_404(id_alumno).id_tutor = request.form['id_tutor']
    db.session.commit()
    flash("Tutor asignado correctamente", "success")
    return redirect(url_for('panel_coordinador'))

@app.route('/coordinador/cambiar-estado/<int:id>')
@requiere_rol('coordinador')
def cambiar_estado(id):
    u = Usuario.query.get_or_404(id); u.bloqueado = not u.bloqueado; u.intentos_fallidos = 0
    db.session.commit(); flash("Estado de usuario actualizado", "success")
    return redirect(url_for('panel_coordinador'))

@app.route('/coordinador/respaldo-manual')
@requiere_rol('coordinador')
def respaldo_manual():
    nom = f"respaldo_manual_{datetime.now().strftime('%Y%m%d_%H%M%S')}.db"
    ruta_destino = os.path.join(CARPETA_RESPALDOS, nom)
    shutil.copy2(RUTA_DB, ruta_destino)
    db.session.add(Auditoria(accion="RESPALDO MANUAL", ip=request.remote_addr, usuario=g.nombre))
    db.session.commit()
    logger.info(f"Respaldo manual creado por '{g.nombre}' desde {request.remote_addr}: {nom}")
    flash(f"✅ Respaldo creado correctamente: {nom}", "success")
    return redirect(url_for('panel_coordinador'))

@app.route('/coordinador/restaurar/<nombre>')
@requiere_rol('coordinador')
def restaurar(nombre):
    ruta_origen = os.path.join(CARPETA_RESPALDOS, nombre)
    if os.path.exists(ruta_origen):
        shutil.copy2(ruta_origen, RUTA_DB)
        db.session.add(Auditoria(accion=f"RESTAURÓ: {nombre}", ip=request.remote_addr, usuario=g.nombre))
        db.session.commit()
        logger.info(f"Base de datos restaurada por '{g.nombre}' desde {request.remote_addr}: {nombre}")
        flash("✅ Base restaurada correctamente", "success")
    else:
        logger.warning(f"'{g.nombre}' intentó restaurar un respaldo inexistente: {nombre}")
        flash("❌ Archivo de respaldo no encontrado", "error")
    return redirect(url_for('panel_coordinador'))

@app.route('/coordinador/config-respaldos', methods=['POST'])
@requiere_rol('coordinador')
def config_respaldos():
    cfg = ConfiguracionRespaldos.query.first(); cfg.activo = 'activo' in request.form; cfg.intervalo_horas = int(request.form['intervalo'])
    db.session.commit()
    logger.info(f"'{g.nombre}' actualizó la configuración de respaldos automáticos: activo={cfg.activo}, intervalo={cfg.intervalo_horas}h")
    flash("✅ Configuración guardada", "success")
    return redirect(url_for('panel_coordinador'))

@app.route('/reporte-general-pdf')
@requiere_rol('coordinador')
def reporte_general_pdf():
    datos = [(u.tipo.upper(), u.credencial, u.nombre_completo, "Bloqueado" if u.bloqueado else "Activo") for u in Usuario.query.all()]
    ruta = generar_pdf(datos, "Reporte General", ["Rol", "Credencial", "Nombre", "Estado"])
    return send_file(ruta, as_attachment=True, download_name="reporte_general.pdf")

# ===================== API (APP MÓVIL / WEAR OS) =====================
def tutoria_a_json(t):
    return {
        'id': t.id,
        'fecha': t.fecha.strftime('%Y-%m-%d'),
        'tema': t.tema,
        'estado': t.estado,
        'observaciones': t.observaciones,
        'alumno': t.alumno.usuario.nombre_completo if t.alumno else None,
    }

@app.route('/api/login', methods=['POST'])
def api_login():
    datos = request.get_json(silent=True) or {}
    cred = (datos.get('credencial') or '').strip()
    passw = (datos.get('contrasena') or '').strip()
    usuario = Usuario.query.filter_by(credencial=cred).first()
    if not usuario or not check_password_hash(usuario.contrasena, passw):
        return {'error': 'Credenciales incorrectas'}, 401
    if usuario.bloqueado:
        return {'error': 'Usuario bloqueado'}, 403
    db.session.add(Auditoria(accion=f"INGRESO (app móvil): {usuario.tipo}", ip=request.remote_addr, usuario=usuario.nombre_completo))
    db.session.commit()
    token = generar_token(usuario)
    return {'token': token, 'uid': usuario.id, 'rol': usuario.tipo, 'nombre': usuario.nombre_completo}

@app.route('/api/perfil')
@requiere_rol_api()
def api_perfil():
    return {'uid': g.uid, 'rol': g.rol, 'nombre': g.nombre}

@app.route('/api/alumno/tutorias')
@requiere_rol_api('alumno')
def api_alumno_tutorias():
    alumno = Alumno.query.filter_by(usuario_id=g.uid).first()
    tutorias = Tutoria.query.filter_by(id_alumno=alumno.id).order_by(Tutoria.fecha.desc()).all()
    return {'tutorias': [tutoria_a_json(t) for t in tutorias]}

@app.route('/api/alumno/tutorias', methods=['POST'])
@requiere_rol_api('alumno')
def api_alumno_solicitar_tutoria():
    alumno = Alumno.query.filter_by(usuario_id=g.uid).first()
    datos = request.get_json(silent=True) or {}
    try:
        fecha = datetime.strptime(datos.get('fecha', ''), '%Y-%m-%d')
    except ValueError:
        return {'error': 'Fecha inválida, usa el formato AAAA-MM-DD'}, 400
    tema = (datos.get('tema') or '').strip()
    if not tema:
        return {'error': 'El tema no puede estar vacío'}, 400
    nueva = Tutoria(id_alumno=alumno.id, id_tutor=alumno.id_tutor, fecha=fecha, tema=tema, estado="Solicitada")
    db.session.add(nueva)
    db.session.commit()
    return {'mensaje': 'Solicitud enviada al tutor', 'tutoria': tutoria_a_json(nueva)}, 201

@app.route('/api/tutor/tutorias')
@requiere_rol_api('tutor')
def api_tutor_tutorias():
    tutor = Tutor.query.filter_by(usuario_id=g.uid).first()
    tutorias = Tutoria.query.filter_by(id_tutor=g.uid).order_by(Tutoria.fecha.desc()).all()
    return {'tutorias': [tutoria_a_json(t) for t in tutorias], 'horario': tutor.horario if tutor else ''}

@app.route('/api/tutor/tutorias/<int:id>/aceptar', methods=['POST'])
@requiere_rol_api('tutor')
def api_tutor_aceptar(id):
    tut = Tutoria.query.get_or_404(id)
    tut.estado = "Confirmada"
    db.session.commit()
    return {'mensaje': 'Tutoría aceptada', 'tutoria': tutoria_a_json(tut)}

@app.route('/api/tutor/tutorias/<int:id>/completar', methods=['POST'])
@requiere_rol_api('tutor')
def api_tutor_completar(id):
    tut = Tutoria.query.get_or_404(id)
    tut.estado = "Realizada"
    db.session.commit()
    return {'mensaje': 'Tutoría marcada como realizada', 'tutoria': tutoria_a_json(tut)}

@app.route('/api/tutor/tutorias/<int:id>/editar', methods=['POST'])
@requiere_rol_api('tutor')
def api_tutor_editar_tutoria(id):
    tut = Tutoria.query.get_or_404(id)
    datos = request.get_json(silent=True) or {}
    try:
        tut.fecha = datetime.strptime(datos.get('fecha', ''), '%Y-%m-%d')
    except ValueError:
        return {'error': 'Fecha inválida, usa el formato AAAA-MM-DD'}, 400
    tema = (datos.get('tema') or '').strip()
    if not tema:
        return {'error': 'El tema no puede estar vacío'}, 400
    tut.tema = tema
    tut.estado = datos.get('estado') or tut.estado
    tut.observaciones = datos.get('observaciones') or ''
    db.session.commit()
    return {'mensaje': 'Tutoría actualizada', 'tutoria': tutoria_a_json(tut)}

@app.route('/api/tutor/alumnos')
@requiere_rol_api('tutor')
def api_tutor_alumnos():
    alumnos = Alumno.query.filter_by(id_tutor=g.uid).all()
    return {'alumnos': [{'id': a.id, 'nombre': a.usuario.nombre_completo} for a in alumnos]}

@app.route('/api/tutor/tutorias', methods=['POST'])
@requiere_rol_api('tutor')
def api_tutor_crear_tutoria():
    datos = request.get_json(silent=True) or {}
    try:
        fecha = datetime.strptime(datos.get('fecha', ''), '%Y-%m-%d')
    except ValueError:
        return {'error': 'Fecha inválida, usa el formato AAAA-MM-DD'}, 400
    tema = (datos.get('tema') or '').strip()
    if not tema:
        return {'error': 'El tema no puede estar vacío'}, 400
    id_alumno = datos.get('id_alumno')
    if not Alumno.query.get(id_alumno):
        return {'error': 'Alumno no encontrado'}, 404
    nueva = Tutoria(id_alumno=id_alumno, id_tutor=g.uid, fecha=fecha, tema=tema, estado="Asignada por tutor")
    db.session.add(nueva)
    db.session.commit()
    return {'mensaje': 'Tutoría creada', 'tutoria': tutoria_a_json(nueva)}, 201

@app.route('/api/tutor/horario', methods=['POST'])
@requiere_rol_api('tutor')
def api_tutor_actualizar_horario():
    tutor = Tutor.query.filter_by(usuario_id=g.uid).first()
    horario = (request.get_json(silent=True) or {}).get('horario', '').strip()
    if not horario:
        return {'error': 'El horario no puede estar vacío'}, 400
    tutor.horario = horario
    db.session.commit()
    return {'mensaje': 'Horario actualizado', 'horario': tutor.horario}

@app.route('/api/coordinador/resumen')
@requiere_rol_api('coordinador')
def api_coordinador_resumen():
    return {
        'tutorias': {
            'total': Tutoria.query.count(),
            'solicitadas': Tutoria.query.filter_by(estado="Solicitada").count(),
            'confirmadas': Tutoria.query.filter_by(estado="Confirmada").count(),
            'realizadas': Tutoria.query.filter_by(estado="Realizada").count(),
            'asignadas': Tutoria.query.filter_by(estado="Asignada por tutor").count(),
        },
        'usuarios': {
            'alumnos': Usuario.query.filter_by(tipo="alumno").count(),
            'tutores': Usuario.query.filter_by(tipo="tutor").count(),
            'activos': Usuario.query.filter_by(bloqueado=False).count(),
            'bloqueados': Usuario.query.filter_by(bloqueado=True).count(),
        }
    }

@app.route('/api/coordinador/usuarios')
@requiere_rol_api('coordinador')
def api_coordinador_usuarios():
    return {'usuarios': [{
        'id': u.id, 'tipo': u.tipo, 'credencial': u.credencial,
        'nombre': u.nombre_completo, 'bloqueado': u.bloqueado
    } for u in Usuario.query.all()]}

@app.route('/api/coordinador/usuarios', methods=['POST'])
@requiere_rol_api('coordinador')
def api_coordinador_crear_usuario():
    datos = request.get_json(silent=True) or {}
    tipo = datos.get('tipo'); cred = (datos.get('credencial') or '').strip()
    nombre = (datos.get('nombre') or '').strip(); clave = datos.get('contrasena') or ''
    if tipo not in ('alumno', 'tutor', 'coordinador') or not cred or not nombre or not clave:
        return {'error': 'Faltan datos o el rol no es válido'}, 400
    if Usuario.query.filter_by(credencial=cred).first():
        return {'error': 'Credencial ya existe'}, 400
    nuevo = Usuario(tipo=tipo, credencial=cred, nombre_completo=nombre, contrasena=generate_password_hash(clave))
    db.session.add(nuevo); db.session.flush()
    if tipo == "alumno": db.session.add(Alumno(usuario_id=nuevo.id, id_tutor=None))
    if tipo == "tutor": db.session.add(Tutor(usuario_id=nuevo.id))
    db.session.add(Auditoria(accion=f"CREÓ USUARIO (app móvil): {cred}", ip=request.remote_addr, usuario=g.nombre))
    db.session.commit()
    return {'mensaje': 'Usuario creado correctamente'}, 201

@app.route('/api/coordinador/usuarios/<int:id_alumno>/asignar-tutor', methods=['POST'])
@requiere_rol_api('coordinador')
def api_coordinador_asignar_tutor(id_alumno):
    alumno = Alumno.query.get_or_404(id_alumno)
    id_tutor = (request.get_json(silent=True) or {}).get('id_tutor')
    if not Usuario.query.filter_by(id=id_tutor, tipo='tutor').first():
        return {'error': 'Tutor no válido'}, 400
    alumno.id_tutor = id_tutor
    db.session.commit()
    return {'mensaje': 'Tutor asignado correctamente'}

@app.route('/api/coordinador/usuarios/<int:id>/estado', methods=['POST'])
@requiere_rol_api('coordinador')
def api_coordinador_cambiar_estado(id):
    u = Usuario.query.get_or_404(id)
    u.bloqueado = not u.bloqueado; u.intentos_fallidos = 0
    db.session.commit()
    return {'mensaje': 'Estado de usuario actualizado', 'bloqueado': u.bloqueado}

@app.route('/api/coordinador/respaldos')
@requiere_rol_api('coordinador')
def api_coordinador_respaldos():
    cfg = ConfiguracionRespaldos.query.first()
    return {
        'respaldos': sorted(os.listdir(CARPETA_RESPALDOS), reverse=True),
        'activo': cfg.activo,
        'intervalo_horas': cfg.intervalo_horas,
    }

@app.route('/api/coordinador/respaldos', methods=['POST'])
@requiere_rol_api('coordinador')
def api_coordinador_respaldo_manual():
    nom = f"respaldo_manual_{datetime.now().strftime('%Y%m%d_%H%M%S')}.db"
    ruta_destino = os.path.join(CARPETA_RESPALDOS, nom)
    shutil.copy2(RUTA_DB, ruta_destino)
    db.session.add(Auditoria(accion="RESPALDO MANUAL (app móvil)", ip=request.remote_addr, usuario=g.nombre))
    db.session.commit()
    logger.info(f"Respaldo manual creado por '{g.nombre}' (app móvil) desde {request.remote_addr}: {nom}")
    return {'mensaje': 'Respaldo creado correctamente', 'nombre': nom}, 201

@app.route('/api/coordinador/respaldos/restaurar', methods=['POST'])
@requiere_rol_api('coordinador')
def api_coordinador_restaurar():
    nombre = (request.get_json(silent=True) or {}).get('nombre', '')
    ruta_origen = os.path.join(CARPETA_RESPALDOS, nombre)
    if not os.path.exists(ruta_origen):
        logger.warning(f"'{g.nombre}' (app móvil) intentó restaurar un respaldo inexistente: {nombre}")
        return {'error': 'Archivo de respaldo no encontrado'}, 404
    shutil.copy2(ruta_origen, RUTA_DB)
    db.session.add(Auditoria(accion=f"RESTAURÓ (app móvil): {nombre}", ip=request.remote_addr, usuario=g.nombre))
    db.session.commit()
    logger.info(f"Base de datos restaurada por '{g.nombre}' (app móvil) desde {request.remote_addr}: {nombre}")
    return {'mensaje': 'Base restaurada correctamente'}

@app.route('/api/coordinador/config-respaldos', methods=['POST'])
@requiere_rol_api('coordinador')
def api_coordinador_config_respaldos():
    datos = request.get_json(silent=True) or {}
    cfg = ConfiguracionRespaldos.query.first()
    cfg.activo = bool(datos.get('activo'))
    try:
        cfg.intervalo_horas = int(datos.get('intervalo_horas'))
    except (TypeError, ValueError):
        return {'error': 'Intervalo inválido'}, 400
    db.session.commit()
    logger.info(f"'{g.nombre}' (app móvil) actualizó la configuración de respaldos automáticos: activo={cfg.activo}, intervalo={cfg.intervalo_horas}h")
    return {'mensaje': 'Configuración guardada'}

@app.route('/api/coordinador/auditoria')
@requiere_rol_api('coordinador')
def api_coordinador_auditoria():
    registros = Auditoria.query.order_by(Auditoria.fecha.desc()).limit(30).all()
    return {'auditoria': [{
        'accion': a.accion, 'fecha': a.fecha.strftime('%Y-%m-%d %H:%M'),
        'ip': a.ip, 'usuario': a.usuario
    } for a in registros]}

@app.errorhandler(500)
def manejar_error_500(error):
    logger.error(f"Error interno en {request.path} desde {request.remote_addr}: {error}")
    return "Ocurrió un error interno, contacta al administrador.", 500

if __name__ == '__main__':
    modo_debug = os.environ.get('FLASK_DEBUG', '0') == '1'
    puerto = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=puerto, debug=modo_debug)
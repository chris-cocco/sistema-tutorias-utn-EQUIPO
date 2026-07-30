from flask import Flask, render_template, request, redirect, url_for, session, flash, send_file
from flask_sqlalchemy import SQLAlchemy
from werkzeug.security import generate_password_hash, check_password_hash
from datetime import datetime, timedelta
from fpdf import FPDF
import shutil, os, threading, time

app = Flask(__name__)
app.config['SECRET_KEY'] = 'clave_segura_sistema_tutorias_2026_utn'
CARPETA_BASE = os.path.dirname(__file__)
RUTA_DB = os.path.join(CARPETA_BASE, "sistema_tutorias.db")
app.config['SQLALCHEMY_DATABASE_URI'] = f'sqlite:///{RUTA_DB}'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['PERMANENT_SESSION_LIFETIME'] = timedelta(minutes=30)

db = SQLAlchemy(app)

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

# ===================== LOGIN =====================
@app.route('/', methods=['GET','POST'])
def login():
    ip = request.remote_addr
    if request.method == 'POST':
        cred = request.form['credencial'].strip()
        passw = request.form['contrasena'].strip()
        usuario = Usuario.query.filter_by(credencial=cred).first()
        if not usuario or not check_password_hash(usuario.contrasena, passw):
            flash("Credenciales incorrectas", "error")
            return redirect(url_for('login'))
        if usuario.bloqueado:
            flash("Usuario bloqueado", "error")
            return redirect(url_for('login'))
        usuario.intentos_fallidos = 0
        db.session.commit()
        session['uid'] = usuario.id
        session['rol'] = usuario.tipo
        session['nombre'] = usuario.nombre_completo
        db.session.add(Auditoria(accion=f"INGRESO: {usuario.tipo}", ip=ip, usuario=usuario.nombre_completo))
        db.session.commit()
        flash(f"Bienvenido {usuario.nombre_completo}", "success")
        return redirect(url_for(f"panel_{usuario.tipo}"))
    return render_template('login.html')

@app.route('/salir')
def salir():
    session.clear()
    return redirect(url_for('login'))

# ===================== ALUMNO =====================
@app.route('/panel-alumno')
def panel_alumno():
    if 'uid' not in session or session['rol'] != 'alumno': return redirect(url_for('login'))
    alumno = Alumno.query.filter_by(usuario_id=session['uid']).first()
    tutorias = Tutoria.query.filter_by(id_alumno=alumno.id).order_by(Tutoria.fecha.desc()).all()
    return render_template('alumno.html', alumno=alumno, tutorias=tutorias)

@app.route('/solicitar-tutoria', methods=['POST'])
def solicitar_tutoria():
    if 'uid' not in session or session['rol'] != 'alumno': return redirect(url_for('login'))
    alumno = Alumno.query.filter_by(usuario_id=session['uid']).first()
    fecha = datetime.strptime(request.form['fecha'], '%Y-%m-%d')
    tema = request.form['tema'].strip()
    if not tema: flash("El tema no puede estar vacío", "error"); return redirect(url_for('panel_alumno'))
    nueva = Tutoria(id_alumno=alumno.id, id_tutor=alumno.id_tutor, fecha=fecha, tema=tema, estado="Solicitada")
    db.session.add(nueva)
    db.session.commit()
    flash("Solicitud enviada al tutor", "success")
    return redirect(url_for('panel_alumno'))

@app.route('/reporte-alumno-pdf')
def reporte_alumno_pdf():
    if 'uid' not in session or session['rol'] != 'alumno': return redirect(url_for('login'))
    alumno = Alumno.query.filter_by(usuario_id=session['uid']).first()
    tutorias = Tutoria.query.filter_by(id_alumno=alumno.id).all()
    datos = [(t.fecha.strftime('%d/%m/%Y'), t.tema, t.estado, t.observaciones[:30]) for t in tutorias]
    ruta = generar_pdf(datos, f"Mis Tutorías - {alumno.usuario.nombre_completo}", ["Fecha", "Tema", "Estado", "Observaciones"])
    return send_file(ruta, as_attachment=True, download_name="mis_tutorias.pdf")

# ==== ✅ AQUÍ VAN LOS REPORTES PARA ALUMNO ====
@app.route('/reportes-alumno')
def reportes_alumno():
    if 'uid' not in session or session['rol'] != 'alumno':
        return redirect(url_for('login'))
    alumno = Alumno.query.filter_by(usuario_id=session['uid']).first()
    mis_tutorias = Tutoria.query.filter_by(id_alumno=alumno.id).all()
    total = len(mis_tutorias)
    realizadas = sum(1 for t in mis_tutorias if t.estado == "Realizada")
    pendientes = sum(1 for t in mis_tutorias if t.estado in ["Solicitada", "Confirmada", "Asignada por tutor"])

    return render_template('reportes_alumno.html',
        total=total, realizadas=realizadas, pendientes=pendientes, tutorias=mis_tutorias)

# ===================== TUTOR =====================
@app.route('/panel-tutor')
def panel_tutor():
    if 'uid' not in session or session['rol'] != 'tutor': return redirect(url_for('login'))
    tutor = Tutor.query.filter_by(usuario_id=session['uid']).first()
    tutorias = Tutoria.query.filter_by(id_tutor=tutor.usuario_id).order_by(Tutoria.fecha.desc()).all()
    alumnos = Alumno.query.filter_by(id_tutor=tutor.usuario_id).all()
    return render_template('tutor.html', tutor=tutor, alumnos=alumnos, tutorias=tutorias)

@app.route('/tutor/aceptar/<int:id>')
def aceptar_tutoria(id):
    if 'uid' not in session or session['rol'] != 'tutor': return redirect(url_for('login'))
    tut = Tutoria.query.get_or_404(id)
    tut.estado = "Confirmada"
    db.session.commit()
    flash("Tutoría aceptada correctamente", "success")
    return redirect(url_for('panel_tutor'))

@app.route('/tutor/editar-tutoria/<int:id>', methods=['GET'])
def form_editar_tutoria(id):
    if 'uid' not in session or session['rol'] != 'tutor': return redirect(url_for('login'))
    tut = Tutoria.query.get_or_404(id)
    return render_template('editar_tutoria.html', tutoria=tut)

@app.route('/tutor/editar-tutoria/<int:id>', methods=['POST'])
def editar_tutoria(id):
    if 'uid' not in session or session['rol'] != 'tutor': return redirect(url_for('login'))
    tut = Tutoria.query.get_or_404(id)
    tut.fecha = datetime.strptime(request.form['fecha'], '%Y-%m-%d')
    tut.tema = request.form['tema']
    tut.estado = request.form['estado']
    tut.observaciones = request.form['observaciones']
    db.session.commit()
    flash("Tutoría actualizada", "success")
    return redirect(url_for('panel_tutor'))

@app.route('/tutor/actualizar-horario', methods=['POST'])
def actualizar_horario():
    if 'uid' not in session or session['rol'] != 'tutor': return redirect(url_for('login'))
    tutor = Tutor.query.filter_by(usuario_id=session['uid']).first()
    tutor.horario = request.form['horario']
    db.session.commit()
    flash("Horario actualizado", "success")
    return redirect(url_for('panel_tutor'))

@app.route('/tutor/crear-tutoria', methods=['POST'])
def crear_tutoria():
    if 'uid' not in session or session['rol'] != 'tutor': return redirect(url_for('login'))
    tutor = Tutor.query.filter_by(usuario_id=session['uid']).first()
    fecha = datetime.strptime(request.form['fecha'], '%Y-%m-%d')
    nueva = Tutoria(id_alumno=request.form['id_alumno'], id_tutor=tutor.usuario_id, fecha=fecha, tema=request.form['tema'], estado="Asignada por tutor")
    db.session.add(nueva)
    db.session.commit()
    flash("Tutoría creada", "success")
    return redirect(url_for('panel_tutor'))

@app.route('/tutor/completar/<int:id>')
def completar_tutoria(id):
    if 'uid' not in session or session['rol'] != 'tutor': return redirect(url_for('login'))
    Tutoria.query.get_or_404(id).estado = "Realizada"
    db.session.commit()
    flash("Tutoría marcada como realizada", "success")
    return redirect(url_for('panel_tutor'))

@app.route('/reporte-tutor-pdf')
def reporte_tutor_pdf():
    if 'uid' not in session or session['rol'] != 'tutor': return redirect(url_for('login'))
    tutor = Tutor.query.filter_by(usuario_id=session['uid']).first()
    tutorias = Tutoria.query.filter_by(id_tutor=tutor.usuario_id).all()
    datos = [(t.alumno.usuario.nombre_completo, t.fecha.strftime('%d/%m/%Y'), t.tema, t.estado) for t in tutorias]
    ruta = generar_pdf(datos, f"Tutorías a mi cargo - {tutor.usuario.nombre_completo}", ["Alumno", "Fecha", "Tema", "Estado"])
    return send_file(ruta, as_attachment=True, download_name="tutorias_tutor.pdf")

# ==== ✅ AQUÍ VAN LOS REPORTES PARA TUTOR ====
@app.route('/reportes-tutor')
def reportes_tutor():
    if 'uid' not in session or session['rol'] != 'tutor':
        return redirect(url_for('login'))
    tutor = Tutor.query.filter_by(usuario_id=session['uid']).first()
    mis_tutorias = Tutoria.query.filter_by(id_tutor=tutor.usuario_id).all()
    mis_alumnos = Alumno.query.filter_by(id_tutor=tutor.usuario_id).count()
    total = len(mis_tutorias)
    realizadas = sum(1 for t in mis_tutorias if t.estado == "Realizada")
    pendientes = sum(1 for t in mis_tutorias if t.estado in ["Solicitada", "Confirmada", "Asignada por tutor"])

    return render_template('reportes_tutor.html',
        total=total, realizadas=realizadas, pendientes=pendientes, alumnos=mis_alumnos, tutorias=mis_tutorias)

# ===================== COORDINADOR =====================
@app.route('/panel-coordinador')
def panel_coordinador():
    if 'uid' not in session or session['rol'] != 'coordinador': return redirect(url_for('login'))
    return render_template('coordinador.html',
        usuarios=Usuario.query.all(), tutorias=Tutoria.query.all(),
        auditoria=Auditoria.query.order_by(Auditoria.fecha.desc()).limit(30).all(),
        respaldos=os.listdir(CARPETA_RESPALDOS), cfg=ConfiguracionRespaldos.query.first())

@app.route('/reportes')
def reportes():
    if 'uid' not in session or session['rol'] != 'coordinador': return redirect(url_for('login'))
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
def crear_usuario():
    if 'uid' not in session or session['rol'] != 'coordinador': return redirect(url_for('login'))
    tipo = request.form['tipo']; cred = request.form['credencial']; nombre = request.form['nombre']; clave = request.form['contrasena']
    if Usuario.query.filter_by(credencial=cred).first(): flash("Credencial ya existe", "error"); return redirect(url_for('panel_coordinador'))
    nuevo = Usuario(tipo=tipo, credencial=cred, nombre_completo=nombre, contrasena=generate_password_hash(clave))
    db.session.add(nuevo); db.session.flush()
    if tipo == "alumno": db.session.add(Alumno(usuario_id=nuevo.id, id_tutor=None))
    if tipo == "tutor": db.session.add(Tutor(usuario_id=nuevo.id))
    db.session.add(Auditoria(accion=f"CREÓ USUARIO: {cred}", ip=request.remote_addr, usuario=session.get('nombre')))
    db.session.commit()
    flash("Usuario creado correctamente", "success")
    return redirect(url_for('panel_coordinador'))

@app.route('/coordinador/asignar-tutor/<int:id_alumno>', methods=['POST'])
def asignar_tutor(id_alumno):
    if 'uid' not in session or session['rol'] != 'coordinador': return redirect(url_for('login'))
    Alumno.query.get_or_404(id_alumno).id_tutor = request.form['id_tutor']
    db.session.commit()
    flash("Tutor asignado correctamente", "success")
    return redirect(url_for('panel_coordinador'))

@app.route('/coordinador/cambiar-estado/<int:id>')
def cambiar_estado(id):
    if 'uid' not in session or session['rol'] != 'coordinador': return redirect(url_for('login'))
    u = Usuario.query.get_or_404(id); u.bloqueado = not u.bloqueado; u.intentos_fallidos = 0
    db.session.commit(); flash("Estado de usuario actualizado", "success")
    return redirect(url_for('panel_coordinador'))

@app.route('/coordinador/respaldo-manual')
def respaldo_manual():
    if 'uid' not in session or session['rol'] != 'coordinador': return redirect(url_for('login'))
    nom = f"respaldo_manual_{datetime.now().strftime('%Y%m%d_%H%M%S')}.db"
    ruta_destino = os.path.join(CARPETA_RESPALDOS, nom)
    shutil.copy2(RUTA_DB, ruta_destino)
    db.session.add(Auditoria(accion="RESPALDO MANUAL", ip=request.remote_addr, usuario=session.get('nombre')))
    db.session.commit()
    flash(f"✅ Respaldo creado correctamente: {nom}", "success")
    return redirect(url_for('panel_coordinador'))

@app.route('/coordinador/restaurar/<nombre>')
def restaurar(nombre):
    if 'uid' not in session or session['rol'] != 'coordinador': return redirect(url_for('login'))
    ruta_origen = os.path.join(CARPETA_RESPALDOS, nombre)
    if os.path.exists(ruta_origen):
        shutil.copy2(ruta_origen, RUTA_DB)
        db.session.add(Auditoria(accion=f"RESTAURÓ: {nombre}", ip=request.remote_addr, usuario=session.get('nombre')))
        flash("✅ Base restaurada correctamente", "success")
    else:
        flash("❌ Archivo de respaldo no encontrado", "error")
    return redirect(url_for('panel_coordinador'))

@app.route('/coordinador/config-respaldos', methods=['POST'])
def config_respaldos():
    if 'uid' not in session or session['rol'] != 'coordinador': return redirect(url_for('login'))
    cfg = ConfiguracionRespaldos.query.first(); cfg.activo = 'activo' in request.form; cfg.intervalo_horas = int(request.form['intervalo'])
    db.session.commit(); flash("✅ Configuración guardada", "success")
    return redirect(url_for('panel_coordinador'))

@app.route('/reporte-general-pdf')
def reporte_general_pdf():
    if 'uid' not in session or session['rol'] != 'coordinador': return redirect(url_for('login'))
    datos = [(u.tipo.upper(), u.credencial, u.nombre_completo, "Bloqueado" if u.bloqueado else "Activo") for u in Usuario.query.all()]
    ruta = generar_pdf(datos, "Reporte General", ["Rol", "Credencial", "Nombre", "Estado"])
    return send_file(ruta, as_attachment=True, download_name="reporte_general.pdf")

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
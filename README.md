# Sistema de Tutorías — UTN

Sistema web para gestionar el proceso de tutorías académicas de la Universidad
Tecnológica de Nayarit. Permite que alumnos soliciten tutorías, tutores las
atiendan y el coordinador administre usuarios, respaldos y reportes.

## Tecnologías

- **Backend:** Python, Flask, Flask-SQLAlchemy, Werkzeug, PyJWT
- **Frontend:** HTML5, CSS3, JavaScript, Bootstrap
- **Base de datos:** SQLite
- **Reportes:** FPDF (generación de PDF)

## Roles del sistema

- **Alumno:** solicita tutorías y consulta su historial.
- **Tutor:** acepta/gestiona tutorías de sus alumnos asignados.
- **Coordinador:** administra usuarios, asigna tutores, genera respaldos y
  consulta reportes y auditoría del sistema.

## Instalación

1. Clonar el repositorio.
2. Instalar dependencias:

```bash
pip install flask flask_sqlalchemy fpdf2 pyjwt pytest
```

3. Ejecutar la aplicación:

```bash
python app.py
```

4. Abrir `http://localhost:5000` en el navegador. La base de datos y los
   usuarios iniciales se crean automáticamente la primera vez que se corre.

## Credenciales de prueba

| Rol | Credencial | Contraseña |
|---|---|---|
| Coordinador | `coordinador` | `clave_coordinador` |
| Tutor | `TUT-000001` | `TUT-000001` |
| Alumno | `TIC-000001` | `TIC-000001` |

## Autenticación

El inicio de sesión usa **JWT** (JSON Web Tokens): al iniciar sesión se genera
un token firmado con expiración de 30 minutos, guardado en una cookie
`httpOnly` (no accesible desde JavaScript). Cada ruta protegida valida el
token y el rol del usuario antes de dar acceso.

## Pruebas de software

El proyecto incluye pruebas automatizadas de caja negra y caja blanca en
`test_app.py`. Para ejecutarlas:

```bash
pytest test_app.py -v
```

El detalle de qué se probó y por qué está documentado en
`PRUEBAS_SOFTWARE.md` / `PRUEBAS_SOFTWARE.pdf`.

## Estructura del proyecto

```
app.py                  # Backend: modelos, rutas y lógica de negocio
templates/               # Vistas Jinja2 (login, paneles por rol, reportes)
static/                  # Archivos estáticos (CSS, JS, imágenes)
test_app.py              # Pruebas de caja negra y caja blanca
PRUEBAS_SOFTWARE.md/.pdf  # Documentación de las pruebas
```

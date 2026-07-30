"""
Pruebas de software del Sistema de Tutorías.

Se dividen en dos técnicas, tal como se pide en la lista de cotejo:

- CAJA NEGRA: se prueban las rutas HTTP solo por su entrada/salida, como lo haría
  un usuario real, sin usar ningún conocimiento del código interno.
- CAJA BLANCA: se diseñan los casos a partir de la lógica interna del decorador
  `requiere_rol` y del manejo del JWT (ramas de token válido, expirado, inválido
  y rol incorrecto), para ejercitar caminos específicos del código.

Ejecutar con: py -m pytest test_app.py -v
"""
import jwt
import pytest
from datetime import datetime, timedelta

from app import app, JWT_ALGORITMO


@pytest.fixture
def cliente():
    app.config['TESTING'] = True
    with app.test_client() as cliente:
        yield cliente


# ===================== CAJA NEGRA =====================
# Solo se conoce el comportamiento esperado desde afuera: qué credenciales
# existen y qué debería pasar al usarlas. No se toca ninguna función interna.

def test_login_credenciales_correctas_redirige_al_panel(cliente):
    respuesta = cliente.post('/', data={'credencial': 'coordinador', 'contrasena': 'clave_coordinador'})
    assert respuesta.status_code == 302
    assert '/panel-coordinador' in respuesta.headers['Location']


def test_login_credenciales_incorrectas_no_deja_entrar(cliente):
    respuesta = cliente.post('/', data={'credencial': 'coordinador', 'contrasena': 'clave_incorrecta'})
    assert respuesta.status_code == 302
    assert respuesta.headers['Location'].rstrip('/') == '' or respuesta.headers['Location'] == '/'


def test_acceso_a_panel_sin_haber_iniciado_sesion_redirige_a_login(cliente):
    respuesta = cliente.get('/panel-coordinador')
    assert respuesta.status_code == 302
    assert respuesta.headers['Location'].rstrip('/') == '' or respuesta.headers['Location'] == '/'


def test_tutor_no_puede_entrar_al_panel_de_coordinador(cliente):
    cliente.post('/', data={'credencial': 'TUT-000001', 'contrasena': 'TUT-000001'})
    respuesta = cliente.get('/panel-coordinador')
    assert respuesta.status_code == 302
    assert '/panel-coordinador' not in respuesta.headers['Location']


# ===================== CAJA BLANCA =====================
# Aquí sí se usa el conocimiento del código interno: se construyen los tokens
# JWT a mano (válido, expirado, corrupto) para ejercitar cada rama del
# decorador `requiere_rol` en app.py.

def test_token_valido_permite_el_acceso(cliente):
    payload = {'uid': 1, 'rol': 'coordinador', 'nombre': 'Coordinador General',
               'exp': datetime.utcnow() + timedelta(minutes=30)}
    token = jwt.encode(payload, app.config['SECRET_KEY'], algorithm=JWT_ALGORITMO)
    cliente.set_cookie('token', token)
    respuesta = cliente.get('/panel-coordinador')
    assert respuesta.status_code == 200


def test_token_expirado_redirige_con_mensaje_de_expiracion(cliente):
    payload = {'uid': 1, 'rol': 'coordinador', 'nombre': 'Coordinador General',
               'exp': datetime.utcnow() - timedelta(minutes=1)}
    token = jwt.encode(payload, app.config['SECRET_KEY'], algorithm=JWT_ALGORITMO)
    cliente.set_cookie('token', token)
    respuesta = cliente.get('/panel-coordinador', follow_redirects=True)
    assert respuesta.status_code == 200
    assert b'expirado' in respuesta.data.lower()


def test_token_con_firma_invalida_es_rechazado(cliente):
    cliente.set_cookie('token', 'esto-no-es-un-token-valido')
    respuesta = cliente.get('/panel-coordinador')
    assert respuesta.status_code == 302
    assert '/panel-coordinador' not in respuesta.headers['Location']


def test_token_valido_pero_con_rol_incorrecto_es_bloqueado(cliente):
    payload = {'uid': 2, 'rol': 'tutor', 'nombre': 'Tutor 1',
               'exp': datetime.utcnow() + timedelta(minutes=30)}
    token = jwt.encode(payload, app.config['SECRET_KEY'], algorithm=JWT_ALGORITMO)
    cliente.set_cookie('token', token)
    respuesta = cliente.get('/panel-coordinador')
    assert respuesta.status_code == 302
    assert '/panel-coordinador' not in respuesta.headers['Location']

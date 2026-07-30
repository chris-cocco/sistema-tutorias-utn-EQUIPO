# Pruebas de Software — Sistema de Tutorías

Este documento resume las dos técnicas de prueba aplicadas al proyecto, tal como se
pide en la lista de cotejo de exposición: **caja negra** y **caja blanca**.

Archivo de pruebas: `test_app.py` (8 pruebas, corridas con `pytest`).
Comando para ejecutarlas: `py -m pytest test_app.py -v`

---

## 1. Pruebas de caja negra

**¿Qué es?** Se prueba el sistema únicamente por su entrada y salida, como lo haría
un usuario real, sin mirar ni usar el código interno. Solo importa: "si mando esto,
¿qué debería devolver el sistema?".

**Cómo se aplicó:** se usó el cliente de pruebas de Flask para hacer peticiones HTTP
reales contra las rutas del sistema (login y panel de coordinador) y verificar el
comportamiento esperado desde afuera.

| Caso de prueba | Entrada | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| Login con credenciales correctas | `coordinador` / `clave_coordinador` | Redirige al panel de coordinador | ✅ Pasó |
| Login con credenciales incorrectas | `coordinador` / clave equivocada | Se queda en el login (rechazado) | ✅ Pasó |
| Acceso a panel sin haber iniciado sesión | `GET /panel-coordinador` sin cookie | Redirige al login | ✅ Pasó |
| Un tutor intenta entrar al panel de coordinador | Login como tutor, luego `GET /panel-coordinador` | Acceso bloqueado | ✅ Pasó |

---

## 2. Pruebas de caja blanca

**¿Qué es?** Se diseñan los casos a partir del código interno, buscando ejercitar
caminos y ramas específicas de la lógica (por ejemplo, cada `except` de una función).

**Cómo se aplicó:** el sistema protege sus rutas con un decorador `requiere_rol`
(en `app.py`) que decodifica un token JWT y tiene 4 caminos posibles internamente:
token válido, token expirado, token corrupto/inválido, y token válido pero con un
rol no autorizado. Se construyó un token JWT a mano para cada caso, firmado con la
misma clave secreta de la aplicación, para forzar cada una de esas 4 ramas.

| Caso de prueba | Rama de código ejercitada | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| Token válido y rol correcto | Camino feliz del decorador | Acceso permitido (200) | ✅ Pasó |
| Token expirado (`exp` en el pasado) | `except jwt.ExpiredSignatureError` | Redirige con mensaje "Tu sesión ha expirado" | ✅ Pasó |
| Token con firma inválida / corrupto | `except jwt.InvalidTokenError` | Acceso rechazado, redirige al login | ✅ Pasó |
| Token válido pero de un tutor accediendo a ruta de coordinador | Verificación de `roles_permitidos` | Acceso bloqueado | ✅ Pasó |

---

## Resultado final

```
8 passed, 0 failed
```

Las 8 pruebas (4 de caja negra + 4 de caja blanca) pasan correctamente, confirmando
que tanto el flujo de autenticación como el control de acceso por rol funcionan
según lo esperado.

# language: es
Característica: Seguridad y Control de Acceso (RBAC)
  Como oficial de seguridad
  Quiero validar las reglas de acceso
  Para que cada rol (jugador, entrenador, administrador) solo entre a su zona

  Antecedentes:
    Dado que el sistema está operativo

  @principal
  Escenario: Un jugador no puede entrar a la administración
    Dado que estoy autenticado como "JUGADOR"
    Cuando intento consultar "/api/admin/retos"
    Entonces el sistema responde con un código de estado 403

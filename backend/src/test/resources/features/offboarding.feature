# language: es
Característica: Escenarios de Offboarding
  Como administrador del sistema
  Quiero bloquear cuentas
  Para revocar el acceso a quien ya no debe entrar

  Antecedentes:
    Dado que el sistema está operativo
    Y que existe un jugador registrado con gamertag "pedro_p" y correo "pedro.p@bedrock.com"

  @principal
  Escenario: El administrador bloquea una cuenta
    Cuando el administrador cambia el estado de la cuenta del jugador a "BLOQUEADO"
    Entonces el sistema responde con un código de estado 200
    Y la cuenta queda en estado "BLOQUEADO" en la base de datos
    Cuando el jugador intenta iniciar sesión
    Entonces el sistema responde con un código de estado 403

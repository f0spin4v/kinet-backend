# language: es
Característica: Gestión del Perfil del Jugador
  Como jugador
  Quiero actualizar mi perfil
  Para mantener mis datos al día

  Antecedentes:
    Dado que el sistema está operativo
    Y que existe un jugador registrado con gamertag "elena_fit" y correo "elena@test.com"

  @principal
  Escenario: Actualizar nombres y gamertag
    Cuando actualizo mi perfil con nombres "Elena María" y gamertag "elena_pro"
    Entonces el sistema responde con un código de estado 200
    Y al consultar mi perfil muestra los nombres "Elena María" y el gamertag "elena_pro"

package com.foxcode.foxweather.rendering

/**
 * Partícula de precipitación individual (lluvia/nieve/granizo/llovizna).
 * Campos en coordenadas de pantalla. x/y en px, vx/vy en px/s.
 */
class RainParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var length: Float,
    var opacity: Float,
    var sway: Float = 0f,
)
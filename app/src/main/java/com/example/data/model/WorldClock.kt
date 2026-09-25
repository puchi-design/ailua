package com.example.data.model

enum class DayPhase(val label: String, val icon: String, val description: String) {
    DAWN("清晨薄曦", "🌅", "微光穿透晨雾，窗外风铃初鸣"),
    MORNING("晨光初现", "☀️", "阳光漫过屋檐，街区逐渐苏醒"),
    NOON("正午微温", "🌤️", "日上三竿，茶馆香气四溢"),
    AFTERNOON("午后漫步", "☕", "阳光洒在庭院，司康初出炉"),
    DUSK("暮色斜照", "🌇", "街角路灯次第亮起，晚风微凉"),
    EVENING("初更向晚", "🏮", "归途人影憧憧，万家灯火温柔"),
    NIGHT("夜阑人静", "🌙", "夜深露重，室内红茶温热，心网守候"),
    LATE_NIGHT("深夜星垂", "🌌", "万籁俱寂，月光书阁老唱片轻轻旋转")
}

enum class WeatherState(val label: String, val icon: String, val atmosphere: String) {
    CLEAR("晴朗", "☀️", "晴空澄澈，微风徐徐"),
    CLOUDY("多云", "⛅", "云层低垂，天光柔和"),
    RAIN("细雨", "🌧️", "秋雨沙沙敲打窗棂，红茶正温"),
    HEAVY_RAIN("阵雨", "⛈️", "雨势急促，水汽弥漫屋檐"),
    SNOW("初雪", "❄️", "素白微霜，世界一片静谧")
}

data class WorldClock(
    val dateLabel: String = "9月25日",
    val minutesOfDay: Int = 21 * 60 + 30, // 21:30 default
    val dayPhase: DayPhase = DayPhase.NIGHT,
    val weather: WeatherState = WeatherState.RAIN
) {
    val timeFormatted: String
        get() {
            val hours = (minutesOfDay / 60) % 24
            val minutes = minutesOfDay % 60
            return "%02d:%02d".format(hours, minutes)
        }
}

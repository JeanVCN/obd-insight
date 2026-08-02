package com.obd.insight.data.elm327

import java.util.Locale

sealed class Elm327Command(val raw: String) {
    data object Reset : Elm327Command("ATZ")
    data object EchoOff : Elm327Command("ATE0")
    data object LinefeedsOff : Elm327Command("ATL0")
    data object SpacesOff : Elm327Command("ATS0")
    data object HeadersOn : Elm327Command("ATH1")
    data object AdaptiveTimingAuto : Elm327Command("ATAT1")
    data object AutoProtocol : Elm327Command("ATSP0")
    data class SetProtocol(val protocol: Int) : Elm327Command("ATSP$protocol")
    data class RawAt(val command: String) : Elm327Command(command)
    data class ReadPid(val mode: Int, val pid: Int) :
        Elm327Command(String.format(Locale.ROOT, "%02X %02X", mode, pid))
    data object ReadDtc : Elm327Command("03")
}

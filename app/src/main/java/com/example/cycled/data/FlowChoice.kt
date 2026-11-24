package com.example.cycled.data

/**
 * The Flow chips on Calendar (D7). Not stored as such: Light/Medium/Heavy become a
 * [PeriodDay], None becomes [DailyLog.noFlow].
 */
enum class FlowChoice(val level: FlowLevel?) {
    NONE(null),
    LIGHT(FlowLevel.LIGHT),
    MEDIUM(FlowLevel.MEDIUM),
    HEAVY(FlowLevel.HEAVY),
    ;

    companion object {
        /** The chip to show selected: none for old Spotting rows (D7), None for noFlow. */
        fun of(flow: FlowLevel?, noFlow: Boolean): FlowChoice? = when (flow) {
            null -> if (noFlow) NONE else null
            FlowLevel.SPOTTING -> null
            FlowLevel.LIGHT -> LIGHT
            FlowLevel.MEDIUM -> MEDIUM
            FlowLevel.HEAVY -> HEAVY
        }
    }
}

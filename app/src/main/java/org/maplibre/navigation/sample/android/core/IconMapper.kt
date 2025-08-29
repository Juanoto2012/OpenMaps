package org.maplibre.navigation.sample.android.core

import org.maplibre.navigation.core.models.ManeuverModifier
import org.maplibre.navigation.core.models.StepManeuver
import org.maplibre.navigation.sample.android.R

object IconMapper {

     fun getIconImage(
        type: StepManeuver.Type?,
        modifier: ManeuverModifier.Type?
    ):Int {

        return when(type) {
            StepManeuver.Type.TURN -> {
                when(modifier) {
                    ManeuverModifier.Type.LEFT -> R.drawable.continue_left
                    ManeuverModifier.Type.RIGHT -> R.drawable.continue_right
                    ManeuverModifier.Type.SLIGHT_RIGHT -> R.drawable.continue_slight_right
                    ManeuverModifier.Type.STRAIGHT -> R.drawable.continue_straight
                    ManeuverModifier.Type.SLIGHT_LEFT -> R.drawable.continue_slight_left
                    ManeuverModifier.Type.SHARP_RIGHT -> R.drawable.turn_sharp_right
                    ManeuverModifier.Type.SHARP_LEFT -> R.drawable.turn_sharp_left
                    else -> R.drawable.continue_straight
                }
            }


            StepManeuver.Type.ARRIVE -> R.drawable.arrive
            StepManeuver.Type.MERGE -> {
                when(modifier) {
                    ManeuverModifier.Type.RIGHT -> R.drawable.merge_right
                    ManeuverModifier.Type.SLIGHT_RIGHT -> R.drawable.merge_slight_right
                    ManeuverModifier.Type.STRAIGHT -> R.drawable.merge_straight
                    ManeuverModifier.Type.SLIGHT_LEFT -> R.drawable.merge_slight_left
                    ManeuverModifier.Type.LEFT -> R.drawable.merge_left
                    else -> R.drawable.merge_straight
                }
            }
            StepManeuver.Type.ON_RAMP -> {
                when(modifier) {
                    ManeuverModifier.Type.RIGHT -> R.drawable.on_ramp_right
                    ManeuverModifier.Type.SHARP_RIGHT -> R.drawable.on_ramp_sharp_right
                    ManeuverModifier.Type.SLIGHT_RIGHT -> R.drawable.on_ramp_slight_right
                    ManeuverModifier.Type.STRAIGHT -> R.drawable.on_ramp_straight
                    ManeuverModifier.Type.SLIGHT_LEFT -> R.drawable.on_ramp_slight_left
                    ManeuverModifier.Type.LEFT -> R.drawable.on_ramp_left
                    ManeuverModifier.Type.SHARP_LEFT -> R.drawable.on_ramp_sharp_left
                    else -> R.drawable.on_ramp_straight
                }
            }
            StepManeuver.Type.OFF_RAMP -> {
                when(modifier) {
                    ManeuverModifier.Type.RIGHT -> R.drawable.off_ramp_right
                    ManeuverModifier.Type.SLIGHT_RIGHT -> R.drawable.off_ramp_slight_right
                    ManeuverModifier.Type.SLIGHT_LEFT -> R.drawable.off_ramp_slight_left
                    ManeuverModifier.Type.LEFT -> R.drawable.off_ramp_left
                    else -> R.drawable.resource_continue
                }
            }
            StepManeuver.Type.FORK -> {
                when(modifier) {
                    ManeuverModifier.Type.RIGHT -> R.drawable.fork_right
                    ManeuverModifier.Type.SLIGHT_RIGHT -> R.drawable.fork_slight_right
                    ManeuverModifier.Type.STRAIGHT -> R.drawable.fork_straight
                    ManeuverModifier.Type.SLIGHT_LEFT -> R.drawable.fork_slight_left
                    ManeuverModifier.Type.LEFT -> R.drawable.fork_left
                    else -> R.drawable.fork_straight
                }
            }

            StepManeuver.Type.END_OF_ROAD -> {
                when(modifier) {
                    ManeuverModifier.Type.RIGHT -> R.drawable.end_of_road_right
                    ManeuverModifier.Type.LEFT -> R.drawable.end_of_road_left
                    else -> R.drawable.resource_continue
                }
            }

            StepManeuver.Type.CONTINUE -> {
                when(modifier) {
                    ManeuverModifier.Type.RIGHT -> R.drawable.continue_right
                    ManeuverModifier.Type.UTURN -> R.drawable.continue_uturn
                    ManeuverModifier.Type.SHARP_RIGHT -> R.drawable.turn_sharp_right
                    ManeuverModifier.Type.SLIGHT_RIGHT -> R.drawable.continue_slight_right
                    ManeuverModifier.Type.STRAIGHT -> R.drawable.resource_continue
                    ManeuverModifier.Type.SLIGHT_LEFT -> R.drawable.continue_slight_left
                    ManeuverModifier.Type.LEFT -> R.drawable.continue_left
                    ManeuverModifier.Type.SHARP_LEFT -> R.drawable.turn_sharp_left
                    else -> R.drawable.continue_straight
                }
            }
            StepManeuver.Type.ROUNDABOUT -> {
                when(modifier) {
                    ManeuverModifier.Type.RIGHT -> R.drawable.roundabout_right
                    ManeuverModifier.Type.SHARP_RIGHT -> R.drawable.roundabout_sharp_right
                    ManeuverModifier.Type.SLIGHT_RIGHT -> R.drawable.roundabout_slight_right
                    ManeuverModifier.Type.STRAIGHT -> R.drawable.roundabout_straight
                    ManeuverModifier.Type.SLIGHT_LEFT -> R.drawable.roundabout_slight_left
                    ManeuverModifier.Type.LEFT -> R.drawable.roundabout_left
                    ManeuverModifier.Type.SHARP_LEFT -> R.drawable.roundabout_sharp_left
                    else -> R.drawable.roundabout_straight
                }
            }
            StepManeuver.Type.ROTARY -> {
                when(modifier) {
                    ManeuverModifier.Type.STRAIGHT -> R.drawable.rotary_straight
                    ManeuverModifier.Type.SHARP_RIGHT -> R.drawable.rotary_sharp_right
                    ManeuverModifier.Type.RIGHT -> R.drawable.rotary_right
                    ManeuverModifier.Type.SLIGHT_RIGHT -> R.drawable.rotary_slight_right
                    ManeuverModifier.Type.SLIGHT_LEFT -> R.drawable.rotary_slight_left
                    ManeuverModifier.Type.LEFT -> R.drawable.rotary_left
                    ManeuverModifier.Type.SHARP_LEFT -> R.drawable.rotary_sharp_left
                    else -> R.drawable.rotary_straight
                }
            }
            StepManeuver.Type.NOTIFICATION -> {
                when(modifier) {
                    ManeuverModifier.Type.STRAIGHT -> R.drawable.notification_straight
                    ManeuverModifier.Type.SHARP_RIGHT -> R.drawable.notificaiton_sharp_right
                    ManeuverModifier.Type.RIGHT -> R.drawable.notification_right
                    ManeuverModifier.Type.SLIGHT_RIGHT -> R.drawable.notification_slight_right
                    ManeuverModifier.Type.SLIGHT_LEFT -> R.drawable.notification_slight_left
                    ManeuverModifier.Type.LEFT -> R.drawable.notification_left
                    ManeuverModifier.Type.SHARP_LEFT -> R.drawable.notification_sharp_left
                    else ->  R.drawable.notification_straight
                }
            }

            StepManeuver.Type.ROUNDABOUT_TURN -> R.drawable.roundabout_straight
            StepManeuver.Type.EXIT_ROUNDABOUT -> R.drawable.roundabout_straight
            StepManeuver.Type.EXIT_ROTARY -> R.drawable.rotary_straight
            else -> R.drawable.continue_straight
        }

    }
}
package com.treefort.guardian.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.bold.Airplane
import com.adamglin.phosphoricons.bold.ArrowLeft
import com.adamglin.phosphoricons.bold.Bell
import com.adamglin.phosphoricons.bold.BookOpenText
import com.adamglin.phosphoricons.bold.Calendar
import com.adamglin.phosphoricons.bold.Campfire
import com.adamglin.phosphoricons.bold.CaretRight
import com.adamglin.phosphoricons.bold.Check
import com.adamglin.phosphoricons.bold.Copy
import com.adamglin.phosphoricons.bold.Eye
import com.adamglin.phosphoricons.bold.EyeSlash
import com.adamglin.phosphoricons.bold.GearSix
import com.adamglin.phosphoricons.bold.House
import com.adamglin.phosphoricons.bold.Info
import com.adamglin.phosphoricons.bold.LockKey
import com.adamglin.phosphoricons.bold.MapPin
import com.adamglin.phosphoricons.bold.Microphone
import com.adamglin.phosphoricons.bold.MicrophoneSlash
import com.adamglin.phosphoricons.bold.MoonStars
import com.adamglin.phosphoricons.bold.PencilSimple
import com.adamglin.phosphoricons.bold.Phone
import com.adamglin.phosphoricons.bold.PhoneCall
import com.adamglin.phosphoricons.bold.PhoneX
import com.adamglin.phosphoricons.bold.Plus
import com.adamglin.phosphoricons.bold.QrCode
import com.adamglin.phosphoricons.bold.Question
import com.adamglin.phosphoricons.bold.Share
import com.adamglin.phosphoricons.bold.ShieldCheck
import com.adamglin.phosphoricons.bold.SignOut
import com.adamglin.phosphoricons.bold.SpeakerHigh
import com.adamglin.phosphoricons.bold.SpeakerSlash
import com.adamglin.phosphoricons.bold.Trash
import com.adamglin.phosphoricons.bold.UserPlus
import com.adamglin.phosphoricons.bold.UsersThree
import com.adamglin.phosphoricons.bold.Warning
import com.adamglin.phosphoricons.bold.X
import com.adamglin.phosphoricons.duotone.Anchor
import com.adamglin.phosphoricons.duotone.Backpack
import com.adamglin.phosphoricons.duotone.Baseball
import com.adamglin.phosphoricons.duotone.Basketball
import com.adamglin.phosphoricons.duotone.Bicycle
import com.adamglin.phosphoricons.duotone.Books
import com.adamglin.phosphoricons.duotone.BowlingBall
import com.adamglin.phosphoricons.duotone.Camera
import com.adamglin.phosphoricons.duotone.Cat
import com.adamglin.phosphoricons.duotone.Compass
import com.adamglin.phosphoricons.duotone.Football
import com.adamglin.phosphoricons.duotone.GameController
import com.adamglin.phosphoricons.duotone.Guitar
import com.adamglin.phosphoricons.duotone.Headset
import com.adamglin.phosphoricons.duotone.Heart
import com.adamglin.phosphoricons.duotone.Leaf
import com.adamglin.phosphoricons.duotone.Medal
import com.adamglin.phosphoricons.duotone.MusicNotes
import com.adamglin.phosphoricons.duotone.PaintBrush
import com.adamglin.phosphoricons.duotone.Palette
import com.adamglin.phosphoricons.duotone.PawPrint
import com.adamglin.phosphoricons.duotone.PianoKeys
import com.adamglin.phosphoricons.duotone.Planet
import com.adamglin.phosphoricons.duotone.Radio
import com.adamglin.phosphoricons.duotone.Robot
import com.adamglin.phosphoricons.duotone.Rocket
import com.adamglin.phosphoricons.duotone.Smiley
import com.adamglin.phosphoricons.duotone.SoccerBall
import com.adamglin.phosphoricons.duotone.Sparkle
import com.adamglin.phosphoricons.duotone.Star
import com.adamglin.phosphoricons.duotone.Sun
import com.adamglin.phosphoricons.duotone.SwimmingPool
import com.adamglin.phosphoricons.duotone.TennisBall
import com.adamglin.phosphoricons.duotone.Tent
import com.adamglin.phosphoricons.duotone.Tree
import com.adamglin.phosphoricons.duotone.Trophy
import com.adamglin.phosphoricons.duotone.User
import com.adamglin.phosphoricons.duotone.Volleyball
import com.adamglin.phosphoricons.fill.BookOpenText as BookOpenTextFill
import com.adamglin.phosphoricons.fill.GearSix as GearSixFill
import com.adamglin.phosphoricons.fill.Phone as PhoneFill
import com.adamglin.phosphoricons.fill.UsersThree as UsersThreeFill
import com.adamglin.phosphoricons.duotone.Bell as BellDuo
import com.adamglin.phosphoricons.duotone.Campfire as CampfireDuo
import com.adamglin.phosphoricons.duotone.House as HouseDuo
import com.adamglin.phosphoricons.duotone.PhoneCall as PhonecallDuo
import com.adamglin.phosphoricons.duotone.ShieldCheck as ShieldcheckDuo
import com.treefort.guardian.theme.BorderInk
import com.treefort.guardian.theme.ColorInk
import com.treefort.guardian.theme.TreeFortTokens

/** UI icons (Phosphor bold; fill for the active tab). DESIGN.md section 7. */
object Ph {
    val Phone = PhosphorIcons.Bold.Phone
    val PhoneFill = PhosphorIcons.Fill.PhoneFill
    val UsersThree = PhosphorIcons.Bold.UsersThree
    val UsersThreeFill = PhosphorIcons.Fill.UsersThreeFill
    val Logbook = PhosphorIcons.Bold.BookOpenText
    val LogbookFill = PhosphorIcons.Fill.BookOpenTextFill
    val Gear = PhosphorIcons.Bold.GearSix
    val GearFill = PhosphorIcons.Fill.GearSixFill
    val Campfire = PhosphorIcons.Bold.Campfire
    val QuietHours = PhosphorIcons.Bold.MoonStars
    val ClaimCode = PhosphorIcons.Bold.QrCode
    val Lock = PhosphorIcons.Bold.LockKey
    val Call = PhosphorIcons.Bold.PhoneCall
    val EndCall = PhosphorIcons.Bold.PhoneX
    val Safelist = PhosphorIcons.Bold.ShieldCheck
    val Back = PhosphorIcons.Bold.ArrowLeft
    val Chevron = PhosphorIcons.Bold.CaretRight
    val Plus = PhosphorIcons.Bold.Plus
    val Trash = PhosphorIcons.Bold.Trash
    val Edit = PhosphorIcons.Bold.PencilSimple
    val Close = PhosphorIcons.Bold.X
    val Check = PhosphorIcons.Bold.Check
    val Warning = PhosphorIcons.Bold.Warning
    val Info = PhosphorIcons.Bold.Info
    val Help = PhosphorIcons.Bold.Question
    val Share = PhosphorIcons.Bold.Share
    val Copy = PhosphorIcons.Bold.Copy
    val Travel = PhosphorIcons.Bold.Airplane
    val Pin = PhosphorIcons.Bold.MapPin
    val Home = PhosphorIcons.Bold.House
    val AddPerson = PhosphorIcons.Bold.UserPlus
    val SignOut = PhosphorIcons.Bold.SignOut
    val Mic = PhosphorIcons.Bold.Microphone
    val MicOff = PhosphorIcons.Bold.MicrophoneSlash
    val Speaker = PhosphorIcons.Bold.SpeakerHigh
    val SpeakerOff = PhosphorIcons.Bold.SpeakerSlash
    val Eye = PhosphorIcons.Bold.Eye
    val EyeOff = PhosphorIcons.Bold.EyeSlash
    val Bell = PhosphorIcons.Bold.Bell
    val Calendar = PhosphorIcons.Bold.Calendar
}

/** The shared avatar vocabulary (backend `avatars.py`): Phosphor icon ids and preset colors. */
object AvatarCatalog {
    data class Group(val title: String, val icons: List<Pair<String, String>>)

    val groups = listOf(
        Group("Sports and games", listOf(
            "soccer-ball" to "Soccer", "basketball" to "Basketball", "baseball" to "Baseball", "tennis-ball" to "Tennis",
            "football" to "Football", "volleyball" to "Volleyball", "bicycle" to "Bicycle", "swimming-pool" to "Swimming",
            "bowling-ball" to "Bowling", "trophy" to "Trophy", "medal" to "Medal",
        )),
        Group("Arts and hobbies", listOf(
            "palette" to "Palette", "paint-brush" to "Paint brush", "guitar" to "Guitar", "piano-keys" to "Piano",
            "books" to "Books", "backpack" to "Backpack", "game-controller" to "Game controller", "camera" to "Camera",
            "music-notes" to "Music",
        )),
        Group("Camp and nature", listOf(
            "tree" to "Treehouse", "campfire" to "Campfire", "tent" to "Tent", "compass" to "Compass",
            "leaf" to "Leaf", "sun" to "Sun", "planet" to "Planet", "anchor" to "Anchor",
        )),
        Group("Pets and play", listOf(
            "cat" to "Cat", "paw-print" to "Paw", "sparkle" to "Sparkle", "star" to "Star",
            "heart" to "Heart", "bell" to "Bell", "smiley" to "Smiley",
        )),
        Group("Tech and gadgets", listOf(
            "phone-call" to "Telephone", "robot" to "Robot", "radio" to "Radio", "headset" to "Headset",
            "rocket" to "Rocket", "house" to "Home", "shield-check" to "Shield", "user" to "Explorer",
        )),
    )

    val presetIds: List<String> = TreeFortTokens.Avatar.Presets.keys.toList()

    fun label(iconId: String): String = groups.flatMap { it.icons }.firstOrNull { it.first == iconId }?.second ?: iconId

    /** Unknown or missing icons render as `user` (ADR 0002). */
    fun vector(id: String?): ImageVector = when (id) {
        "anchor" -> PhosphorIcons.Duotone.Anchor
        "backpack" -> PhosphorIcons.Duotone.Backpack
        "baseball" -> PhosphorIcons.Duotone.Baseball
        "basketball" -> PhosphorIcons.Duotone.Basketball
        "bell" -> PhosphorIcons.Duotone.BellDuo
        "bicycle" -> PhosphorIcons.Duotone.Bicycle
        "books" -> PhosphorIcons.Duotone.Books
        "bowling-ball" -> PhosphorIcons.Duotone.BowlingBall
        "camera" -> PhosphorIcons.Duotone.Camera
        "campfire" -> PhosphorIcons.Duotone.CampfireDuo
        "cat" -> PhosphorIcons.Duotone.Cat
        "compass" -> PhosphorIcons.Duotone.Compass
        "football" -> PhosphorIcons.Duotone.Football
        "game-controller" -> PhosphorIcons.Duotone.GameController
        "guitar" -> PhosphorIcons.Duotone.Guitar
        "headset" -> PhosphorIcons.Duotone.Headset
        "heart" -> PhosphorIcons.Duotone.Heart
        "house" -> PhosphorIcons.Duotone.HouseDuo
        "leaf" -> PhosphorIcons.Duotone.Leaf
        "medal" -> PhosphorIcons.Duotone.Medal
        "music-notes" -> PhosphorIcons.Duotone.MusicNotes
        "paint-brush" -> PhosphorIcons.Duotone.PaintBrush
        "palette" -> PhosphorIcons.Duotone.Palette
        "paw-print" -> PhosphorIcons.Duotone.PawPrint
        "phone-call" -> PhosphorIcons.Duotone.PhonecallDuo
        "piano-keys" -> PhosphorIcons.Duotone.PianoKeys
        "planet" -> PhosphorIcons.Duotone.Planet
        "radio" -> PhosphorIcons.Duotone.Radio
        "robot" -> PhosphorIcons.Duotone.Robot
        "rocket" -> PhosphorIcons.Duotone.Rocket
        "shield-check" -> PhosphorIcons.Duotone.ShieldcheckDuo
        "smiley" -> PhosphorIcons.Duotone.Smiley
        "soccer-ball" -> PhosphorIcons.Duotone.SoccerBall
        "sparkle" -> PhosphorIcons.Duotone.Sparkle
        "star" -> PhosphorIcons.Duotone.Star
        "sun" -> PhosphorIcons.Duotone.Sun
        "swimming-pool" -> PhosphorIcons.Duotone.SwimmingPool
        "tennis-ball" -> PhosphorIcons.Duotone.TennisBall
        "tent" -> PhosphorIcons.Duotone.Tent
        "tree" -> PhosphorIcons.Duotone.Tree
        "trophy" -> PhosphorIcons.Duotone.Trophy
        "volleyball" -> PhosphorIcons.Duotone.Volleyball
        else -> PhosphorIcons.Duotone.User
    }

    fun preset(token: String?): TreeFortTokens.Avatar.Preset =
        TreeFortTokens.Avatar.Presets[token] ?: TreeFortTokens.Avatar.Presets.getValue("slate-mid")

    /** Deterministic default preset for an id that has no avatar yet (DESIGN.md section 8). */
    fun defaultPreset(seed: String): String = presetIds[Math.floorMod(seed.hashCode(), presetIds.size)]
}

/** Draws a duotone Phosphor glyph: outline in [outline], the duotone layer in [fill] at full opacity. */
@Composable
fun DuotoneGlyph(icon: ImageVector, outline: Color, fill: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val scale = size.width / icon.viewportWidth
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            fun walk(g: VectorGroup) {
                for (node in g) {
                    when (node) {
                        is VectorGroup -> walk(node)
                        is VectorPath -> {
                            val duotoneLayer = node.fillAlpha < 0.99f
                            drawPath(node.pathData.toPath(), if (duotoneLayer) fill else outline)
                        }
                    }
                }
            }
            walk(icon.root)
        }
    }
}

/**
 * Avatar: squircle frame in the preset color, a cream tile, and the duotone glyph (DESIGN.md
 * section 8). [name] becomes the accessibility label. At `xs` the tile is dropped.
 */
@Composable
fun PhosphorAvatar(
    icon: String?,
    color: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = TreeFortTokens.Avatar.SizeMd,
) {
    val preset = AvatarCatalog.preset(color ?: TreeFortTokens.Avatar.DefaultPreset)
    val frame = RoundedCornerShape(size * TreeFortTokens.Avatar.CornerRatio)
    val inset = size * TreeFortTokens.Avatar.TileInsetRatio
    val glyphBox = size * TreeFortTokens.Avatar.GlyphRatio
    val vector = AvatarCatalog.vector(icon ?: TreeFortTokens.Avatar.DefaultIcon)
    val showTile = size > TreeFortTokens.Avatar.SizeXs
    Box(
        modifier
            .semantics { contentDescription = name }
            .size(size)
            .background(preset.background, frame)
            .border(TreeFortTokens.Border.Std, BorderInk, frame),
        contentAlignment = Alignment.Center,
    ) {
        if (showTile) {
            val tileShape = RoundedCornerShape((size - inset * 2) * 0.22f)
            Box(
                Modifier
                    .padding(inset)
                    .matchParentSize()
                    .background(TreeFortTokens.Avatar.Tile, tileShape)
                    .border(TreeFortTokens.Border.Std, ColorInk, tileShape)
            )
            DuotoneGlyph(vector, outline = TreeFortTokens.Avatar.Glyph, fill = preset.fill, modifier = Modifier.size(glyphBox))
        } else {
            DuotoneGlyph(vector, outline = onColorFor(preset.background), fill = preset.fill, modifier = Modifier.size(size * 0.7f))
        }
    }
}

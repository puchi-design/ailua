package com.example.data.model

enum class LetterType(val label: String, val icon: String) {
    LETTER("信笺", "✉️"),
    POSTCARD("明信片", "🎑"),
    NOTE("便签纸条", "📝"),
    PACKAGE_NOTE("心意礼笺", "🎁")
}

enum class LetterDeliveryState {
    SCHEDULED,
    DELIVERED,
    OPENED,
    REPLIED
}

data class Letter(
    val id: String,
    val characterId: String,
    val senderName: String,
    val subject: String,
    val body: String,
    val createdAtVirtualTime: String,
    val deliverAtVirtualTimeMinutes: Int,
    val deliverAtVirtualTimeString: String = "22:30",
    var deliveryState: LetterDeliveryState = LetterDeliveryState.SCHEDULED,
    val letterType: LetterType = LetterType.LETTER,
    val attachmentReference: String? = null,
    val relatedLifeEventId: String? = null,
    val paperColorHex: Long = 0xFFFDFBF7
)

val Letter.isRead: Boolean
    get() = deliveryState == LetterDeliveryState.OPENED || deliveryState == LetterDeliveryState.REPLIED

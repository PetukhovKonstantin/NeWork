package ru.netology.nework.models

import android.net.Uri
import ru.netology.nework.dto.AttachmentType
import java.io.File

data class MediaModel(val uri: Uri? = null, val file: File? = null, val attachmentType: AttachmentType? = null)
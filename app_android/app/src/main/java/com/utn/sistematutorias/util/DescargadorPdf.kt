package com.utn.sistematutorias.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import okhttp3.ResponseBody
import java.io.File

/**
 * Guarda el PDF que devuelve el backend en el cache de la app y abre el visor
 * de PDF del dispositivo con un FileProvider (evita exponer una ruta file:// directa).
 */
fun abrirPdfDescargado(contexto: Context, cuerpo: ResponseBody, nombreArchivo: String) {
    val carpeta = File(contexto.cacheDir, "reportes").apply { mkdirs() }
    val archivo = File(carpeta, nombreArchivo)
    cuerpo.byteStream().use { entrada ->
        archivo.outputStream().use { salida -> entrada.copyTo(salida) }
    }

    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    contexto.startActivity(intent)
}

package com.zj.im.utils.log.logger

import android.app.Application
import java.io.*
import java.lang.IllegalArgumentException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.text.StringBuilder

/**
 * Created by ZJJ
 */
@Suppress("unused")
class FileUtils private constructor(private val homePath: String) {

    fun getHomePathFile(): File? {
        var homeFile: File? = null
        try {
            homeFile = File(homePath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (homeFile?.exists() == false) {
            homeFile.mkdirs()
        }
        return homeFile
    }

    /**
     * save some files
     *
     * @param path     your file path,the path file in your home cache,and make it build;
     * @param fileName your fileName, you must append the expand name;
     */
    fun save(path: String?, fileName: String?, text: String, isAppend: Boolean) {
        checkValidate(fileName)
        if (homePath.isEmpty() || fileName.isNullOrEmpty()) return
        val file = if (path.isNullOrEmpty()) getHomePathFile() else File(homePath, path)
        if (file?.exists() == false) {
            file.mkdirs()
        }
        val dataFile = File(file, "$fileName.txt")
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (dataFile.exists() && !dataFile.isDirectory) {
            saveTxt(dataFile, text, isAppend)
        }
    }

    private fun saveTxt(dataFile: File, params: String, append: Boolean): Boolean {
        return try {
            val fos = FileOutputStream(dataFile, append)
            fos.write(params.toByteArray())
            fos.close()
            fos.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getTxt(dataFile: File?): String {
        if (dataFile == null || !dataFile.exists()) return ""
        return try {
            dataFile.inputStream().use { fis ->
                val sb = StringBuilder()
                val read = InputStreamReader(fis, "UTF-8")// 考虑到编码格式
                val bufferedReader = BufferedReader(read)
                var lineTxt: String? = null
                while ({ lineTxt = bufferedReader.readLine();lineTxt }() != null) {
                    sb.append(lineTxt)
                }
                String(sb)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "file not found or read ioe"
        }
    }

    fun getFile(path: String?, fileName: String?): File? {
        checkValidate(fileName)
        return try {
            val home = File(homePath)
            val pathFile = if (path.isNullOrEmpty()) home else File(home, path)
            return File(pathFile, "$fileName.txt")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteFolder(path: String): Boolean {
        val file = File(path)
        return if (!file.exists()) {
            true
        } else {
            if (file.isFile) {
                delete(path)
            } else {
                deleteDirectory(path)
            }
        }
    }

    /**
     * delete the folder form cursors
     * @param path the path of file or directory
     * @return if it deleted success ,true or false
     */
    private fun deleteDirectory(path: String): Boolean {
        var filePath = path
        var flag: Boolean
        if (!filePath.endsWith(File.separator)) {
            filePath += File.separator
        }
        val dirFile = File(filePath)
        if (!dirFile.exists() || !dirFile.isDirectory) {
            return false
        }
        flag = true
        val files = dirFile.listFiles() ?: return true
        for (i in files.indices) {
            if (files[i].isFile) {
                flag = delete(files[i].absolutePath)
                if (!flag) break
            } else {
                flag = deleteDirectory(files[i].absolutePath)
                if (!flag) break
            }
        }
        return if (!flag) false else dirFile.delete()
    }

    private fun delete(path: String): Boolean {
        val file: File? = File(path)
        return if (file != null && file.isFile) {
            file.delete()
        } else false
    }


    /**
     * create your disk name with your cache home;
     */
    companion object {

        private var DISK: String = ""

        fun init(appContext: Application?, diskPathName: String): FileUtils {
            DISK = appContext?.externalCacheDir?.absolutePath + File.separator
            return FileUtils(DISK + diskPathName)
        }

        fun getHomePath(path: String): String {
            return DISK + path
        }

        fun compressToZip(sourceFilePath: String, zipFilePath: String, zipFilename: String) {
            val sourceFile = File(sourceFilePath)
            val zipPath = File(zipFilePath)
            if (!zipPath.exists()) {
                if (!zipPath.mkdirs()) {
                    return
                }
            }
            val zipFile = File(zipPath.toString() + File.separator + zipFilename)
            try {
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    writeZip(sourceFile, "", zos)
                    deleteDir(sourceFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException(e.message, e.cause)
            }
        }

        private fun writeZip(file: File, path: String, zos: ZipOutputStream) {
            var parentPath = path
            if (file.isDirectory) {
                parentPath += file.name + File.separator
                val files = file.listFiles() ?: return
                for (f in files) {
                    writeZip(f, parentPath, zos)
                }
            } else {
                try {
                    BufferedInputStream(FileInputStream(file)).use { bis ->
                        val zipEntry = ZipEntry(parentPath + file.name)
                        zos.putNextEntry(zipEntry)
                        var len: Int = -1
                        val buffer = ByteArray(1024)
                        while ({ len = bis.read(buffer, 0, buffer.size);len }() != -1) {
                            zos.write(buffer, 0, len)
                            zos.flush()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw RuntimeException(e.message, e.cause)
                }
            }
        }

        private fun deleteDir(dir: File): Boolean {
            if (dir.isDirectory) {
                val children = dir.list() ?: return true
                for (i in 0 until children.size) {
                    val success = deleteDir(File(dir, children[i]))
                    if (!success) {
                        return false
                    }
                }
            }
            return dir.delete()
        }
    }

    private fun checkValidate(fileName: String?) {
        if (fileName.isNullOrEmpty() || fileName.contains(".")) throw IllegalArgumentException("file name is non-null and can not contain with suffix")
    }
}

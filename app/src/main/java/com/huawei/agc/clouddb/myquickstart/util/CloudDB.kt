package com.huawei.agc.clouddb.myquickstart.util

import android.util.Log
import com.huawei.agc.clouddb.myquickstart.model.Book
import com.huawei.agc.clouddb.myquickstart.model.getObjectTypeInfo
import com.huawei.agconnect.cloud.database.AGConnectCloudDB
import com.huawei.agconnect.cloud.database.CloudDBZone
import com.huawei.agconnect.cloud.database.CloudDBZoneConfig
import com.huawei.agconnect.cloud.database.CloudDBZoneObjectList
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery
import com.huawei.agconnect.cloud.database.CloudDBZoneSnapshot
import com.huawei.agconnect.cloud.database.CloudDBZoneTask
import com.huawei.agconnect.cloud.database.ListenerHandler
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException
import java.util.*
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

object CloudDB {

    private var agConnectCloudDB: AGConnectCloudDB = AGConnectCloudDB.getInstance()
    private var cloudDBZone: CloudDBZone? = null
    private var register: ListenerHandler? = null
    private var config: CloudDBZoneConfig? = null

    private var uiCallBack: UiCallBack? = null

    private var bookIndex = 0

    private val readWriteLock: ReadWriteLock = ReentrantReadWriteLock()

    /**
     * Call AGConnectCloudDB.createObjectType to init schema
     */
    fun createObjectType() {
        try {
            agConnectCloudDB.createObjectType(getObjectTypeInfo())
        } catch (e: AGConnectCloudDBException) {
            Log.w(CLOUD_TAG, "createObjectType: ${e.message}")
        }
    }

    /**
     * Call AGConnectCloudDB.openCloudDBZone to open a cloudDBZone.
     * We set it with cloud cache mode, and data can be store in local storage
     */
    fun openCloudDBZone() {
        config = CloudDBZoneConfig(
            ZONE_NAME,
            CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
            CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC
        )

        config?.let {
            it.persistenceEnabled = true
            try {
                cloudDBZone = agConnectCloudDB.openCloudDBZone(it, true)
            } catch (e: AGConnectCloudDBException) {
                Log.w(CLOUD_TAG, "openCloudDBZone: ${e.message}")
            }
        }
    }

    /**
     * Call AGConnectCloudDB.closeCloudDBZone
     */
    fun closeCloudDBZone() {
        try {
            register?.remove()
            agConnectCloudDB.closeCloudDBZone(cloudDBZone)
        } catch (e: AGConnectCloudDBException) {
            Log.w(CLOUD_TAG, "closeCloudDBZone: " + e.message)
        }
    }

    /**
     * Query all books in storage from cloud side with CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
     */
    fun getAll()  {
        if (cloudDBZone == null) {
            Log.w(CLOUD_TAG, "CloudDBZone is null, try re-open it")
            return
        }
        cloudDBZone?.let {
            val queryTask: CloudDBZoneTask<CloudDBZoneSnapshot<Book>> = it.executeQuery(
                CloudDBZoneQuery.where(Book::class.java),
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
            )
            queryTask
                .addOnSuccessListener { snapshot ->
                    processQueryResult(snapshot)
                }
                .addOnFailureListener { error ->
                    Log.e(CLOUD_TAG, error.message.toString())
                }
        }
    }

    private fun processQueryResult(snapshot: CloudDBZoneSnapshot<Book>) {
        val bookInfoCursor: CloudDBZoneObjectList<Book> = snapshot.snapshotObjects
        val bookList: MutableList<Book> = ArrayList()
        try {
            while (bookInfoCursor.hasNext()) {
                val book: Book = bookInfoCursor.next()
                bookList.add(book)
                updateBookIndex(book)
            }
        } catch (e: AGConnectCloudDBException) {
            Log.w(CLOUD_TAG, "processQueryResult: ${e.message}")
        }
        snapshot.release()
        uiCallBack?.onStart(bookList)
    }

    /**
     * Delete book
     *
     * @param bookList books selected by user
     */
    fun deleteBook(bookList: List<Book>) {
        if (cloudDBZone == null) {
            Log.w(CLOUD_TAG, "CloudDBZone is null, try re-open it")
            return
        }
        val deleteTask = cloudDBZone?.executeDelete(bookList)
        deleteTask?.let { task ->
            task.addOnSuccessListener {
            }.addOnFailureListener {
                Log.e(CLOUD_TAG, "Delete book is failed: ${it.message}")
            }
        }
    }

    /**
     * Insert book
     *
     * @param book book added or modified from local
     */
    fun insertBook(book: Book) {
        if (cloudDBZone == null) {
            Log.w(CLOUD_TAG, "CloudDBZone is null, try re-open it")
            return
        }
        cloudDBZone?.let {
            val insertTask: CloudDBZoneTask<Int> = it.executeUpsert(book)
            insertTask.addOnSuccessListener { cloudDBZoneResult ->
                Log.d(CLOUD_TAG, "inserted $cloudDBZoneResult records")
                uiCallBack?.onAddItem(book)
            }.addOnFailureListener { error ->
                Log.e(CLOUD_TAG, "onFailure: ${error.message}")
            }
        }
    }

    /**
     * Get max id of books
     *
     * @return max book id
     */
    fun getBookIndex(): Int {
        return try {
            readWriteLock.readLock().lock()
            bookIndex
        } finally {
            readWriteLock.readLock().unlock()
        }
    }

    private fun updateBookIndex(book: Book) {
        try {
            readWriteLock.writeLock().lock()
            book.id?.let {
                if (bookIndex < it) {
                    bookIndex = it
                }
            }
        } finally {
            readWriteLock.writeLock().unlock()
        }
    }

    /**
     * Add a callback to update book info list
     *
     * @param uiCallBack callback to update book list
     */
    fun addCallBacks(uiCallBack: UiCallBack) {
        this.uiCallBack = uiCallBack
    }

    /**
     * Call back to update ui
     */
    interface UiCallBack {
        fun onStart(books: List<Book>?)
        fun onAddItem(book: Book)
    }
}
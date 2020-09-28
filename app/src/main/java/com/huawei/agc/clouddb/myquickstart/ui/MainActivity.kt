package com.huawei.agc.clouddb.myquickstart.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.huawei.agc.clouddb.myquickstart.R
import com.huawei.agc.clouddb.myquickstart.model.Book
import com.huawei.agc.clouddb.myquickstart.util.CloudDB
import com.huawei.agc.clouddb.myquickstart.util.HUAWEI_ID_SIGN_IN
import com.huawei.agc.clouddb.myquickstart.util.MAIN_ACTIVITY_TAG
import com.huawei.agc.clouddb.myquickstart.view.ItemAdapter
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.HwIdAuthProvider
import com.huawei.hms.support.api.entity.auth.Scope
import com.huawei.hms.support.api.entity.hwid.HwIDConstant
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), CloudDB.UiCallBack {

    private var itemAdapter: ItemAdapter? = null
    private var items: MutableList<Book>? = null

    //menu buttons
    private var logOutMenuItem: MenuItem? = null
    private var logInMenuItem: MenuItem? = null
    private var addMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        itemList?.setHasFixedSize(true)
        itemList?.layoutManager = LinearLayoutManager(this)
        items = ArrayList()

        //Refresh data from CloudDB by "swipeRefreshLayout"
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            CloudDB.getAll()
        }
    }

    /**
     * 1. Obtain accessToken in order to logIn, you must to be authorized to use all functions "CRUD".
     *
     * 2. Initialization CloudDB, have done by globally init i.e. [CloudDBQuickStartApplication].
     *
     * 4. Create Object Type. (Can be imported from the console [ObjectTypeInfoHelper])
     *    You must implement zone/dataType on the console and download java files.
     *
     * 5. Open CloudDB Zone. (This will be closed [onDestroy])
     *
     * 6. Fetch all data from the CloudDB and push it to the recyclerView.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //Obtain accessToken in order to logIn and logIn
        if (requestCode == HUAWEI_ID_SIGN_IN) {
            val authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data)
            if (authHuaweiIdTask.isSuccessful) {
                val huaweiAccount = authHuaweiIdTask.result
                val accessToken = huaweiAccount.accessToken
                Log.i(MAIN_ACTIVITY_TAG, "accessToken: $accessToken")
                val credential = HwIdAuthProvider.credentialWithToken(accessToken)
                AGConnectAuth.getInstance().signIn(credential).addOnSuccessListener {
                    val user = it.user
                    Toast.makeText(this@MainActivity, "Hello, ${user.displayName}", Toast.LENGTH_LONG)
                        .show()

                        CloudDB.addCallBacks(this)
                        // Get AGConnectCloudDB ObjectTypeInfo
                        CloudDB.createObjectType()
                        //Create the Cloud DB zone, And open CloudDB
                        CloudDB.openCloudDBZone()
                        //fetchDataFromDb()
                        CloudDB.getAll()
                }.addOnFailureListener {
                        Log.e(MAIN_ACTIVITY_TAG, "onFailure: ${it.message}")
                        Toast.makeText(this@MainActivity, "Failed", Toast.LENGTH_LONG).show()
                    }
            }
        } else {
            Toast.makeText(this@MainActivity, "HwID signIn failed", Toast.LENGTH_LONG).show()
            Log.e(MAIN_ACTIVITY_TAG, "HwID signIn failed")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        logOutMenuItem = menu?.findItem(R.id.menu_logout_button)
        logInMenuItem = menu?.findItem(R.id.menu_login_button)
        addMenuItem = menu?.findItem(R.id.menu_add_button)
        logIn()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_login_button -> {
                logIn()
                true
            }
            R.id.menu_logout_button -> {
                logOut()
                true
            }
            R.id.menu_add_button -> {
                addItem()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Add an new Item
     */
    private fun addItem() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val view = inflater?.inflate(R.layout.popup, null)

        builder.setView(view)
        val alertDialog = builder.create()
        alertDialog?.show()

        val saveButton: Button? = view?.findViewById(R.id.saveButton)
        val title: EditText? = view?.findViewById(R.id.titleBook)
        val description: EditText? = view?.findViewById(R.id.descriptionBook)
        val titlePage: TextView? = view?.findViewById(R.id.titlePage)

        titlePage?.text = getString(R.string.enter_book)
        saveButton?.text = getString(R.string.save)

        saveButton?.setOnClickListener {
            val item = Book()
            item.id = CloudDB.getBookIndex() + 1
            item.bookName = title?.text.toString().trim()
            item.description = description?.text.toString().trim()

            //save the new item to CloudDB
            CloudDB.insertBook(item)
            alertDialog?.dismiss()
        }
    }

    /**
     * LogOut
     */
    private fun logOut() {
        val auth = AGConnectAuth.getInstance()
        auth.signOut()

        logOutMenuItem?.isVisible = false
        logInMenuItem?.isVisible = true
    }

    /**
     * LogIn, next step is to obtain info [onActivityResult]
     */
    private fun logIn() {
        logOut()

        val huaweiIdAuthParamsHelper = HuaweiIdAuthParamsHelper(
            HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM
        )
        val scopeList: MutableList<Scope> = ArrayList()
        scopeList.add(Scope(HwIDConstant.SCOPE.ACCOUNT_BASEPROFILE))
        huaweiIdAuthParamsHelper.setScopeList(scopeList)
        val authParams = huaweiIdAuthParamsHelper.setAccessToken().createParams()
        val service = HuaweiIdAuthManager.getService(this@MainActivity, authParams)
        startActivityForResult(service.signInIntent, HUAWEI_ID_SIGN_IN)

        logInMenuItem?.isVisible = false
        logOutMenuItem?.isVisible = true
    }

    /**
     * Close the CloudDBZone
     */
    override fun onDestroy() {
        super.onDestroy()
        CloudDB.closeCloudDBZone()
    }

    /**
     * Call back to add an item
     */
    override fun onStart(books: List<Book>?) {
        items = books as MutableList<Book>?
        itemAdapter = items?.let { ItemAdapter(this, it as ArrayList<Book>) }
        itemList?.adapter = itemAdapter
        itemAdapter?.notifyDataSetChanged()
    }

    override fun onAddItem(book: Book) {
        items?.let {
            if (!it.contains(book))  items?.add(book)
        }
        itemAdapter?.notifyDataSetChanged()
    }
}

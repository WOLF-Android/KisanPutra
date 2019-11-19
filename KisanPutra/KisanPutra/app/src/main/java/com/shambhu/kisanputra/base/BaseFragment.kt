package com.shambhu.kisanputra.base

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.shambhu.kisanputra.BuildConfig
import com.shambhu.kisanputra.R
import com.shambhu.kisanputra.data.models.UserData
import com.shambhu.kisanputra.utils.SafeClickListener
import com.shambhu.kisanputra.utils.StaticData
import kotlinx.android.synthetic.main.activity_login.*
import java.io.File
import java.util.HashMap
import kotlin.collections.ArrayList
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

open class BaseFragment : Fragment(), View.OnClickListener {

    val TAG = BaseFragment::class.java.simpleName
    val REQ_IMAGE_PERM = 22
    private val permissionList = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    var mDialog: AlertDialog? = null
   // lateinit var transferUtility : TransferUtility
    lateinit var activity: BaseActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity= getActivity() as BaseActivity;
    }


  /*  override fun onAttach(context: Context?) {
        super.onAttach(LocaleHelper.onAttach(context!!, "en"))

    }*/



    /** function will be implemented in Child classes whenever required  **/
    override fun onClick(v: View?) { }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

    }

    fun handleProgressLoader(isLoading: Boolean){
        if (isLoading) {
            progress_layout.visibility = View.VISIBLE
            activity.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }else {
            progress_layout.visibility = View.GONE
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }

    fun handleError(error: String){
        if(!TextUtils.isEmpty(error)){
            var message = error
            if (error.equals(StaticData.TIMEOUT)) {
                message =getString(R.string.timeout_error)
            } else if (error.equals(StaticData.UNDEFINED)) {
                message =getString(R.string.undefined_error)
            } else if (error.equals(StaticData.NODATA_FOUND))
                message=getString(R.string.no_data_found)
            showAlertDialog(message, "", "", getString(R.string.ok), null)
        }
    }

    /** In case no network is available, alert the user with a message **/
    fun showNetworkIssue() {

        showAlertDialog(
            getString(R.string.no_internet),
            getString(R.string.retry),
            getString(R.string.ok),
            "",
            DialogInterface.OnClickListener { _, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> retryAction()
                    DialogInterface.BUTTON_NEGATIVE -> failureAccepted()
                }
            })
    }

    fun showAlertDialog(
        message: String?,
        positive: String,
        negative: String,
        neutral: String,
        listener: DialogInterface.OnClickListener?) {

        val builder = AlertDialog.Builder(activity)
        builder.setTitle(getString(R.string.app_name))
        builder.setMessage(message)
        if (!TextUtils.isEmpty(positive)) {
            builder.setPositiveButton(positive, listener)
        }
        if (!TextUtils.isEmpty(negative)) {
            builder.setNegativeButton(negative, listener)
        }
        if (!TextUtils.isEmpty(neutral)) {
            builder.setNeutralButton(neutral, listener)
        }
        if (mDialog != null) {
            mDialog?.dismiss()
        }
        mDialog = builder.create()
        mDialog?.show()
    }

    /** UserData has opted to retry API calling which failed earlier due to Network unavailability **/
    open fun retryAction() {

    }

    /** UserData has accepted API calling failure due to Network unavailability **/
    open fun failureAccepted() {

    }

    fun hideKeyBoard() {
        try {
            val imm =activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.findViewById<View>(android.R.id.content).windowToken, 0)
        } catch (e: Exception) {
            printLog("BaseActivity", e.toString())
        }

    }

    fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    fun printLog(tag:String, log:String){
        if(BuildConfig.DEBUG)
            Log.d(tag, log)
    }

    /** Image Selection Tasks **/

    fun verifyImagePermissions() {
        val requiredPermission = ArrayList<String>()
        for (perm in permissionList) {
            if (ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                requiredPermission.add(perm)
            }
        }
        if (!requiredPermission.isEmpty()) {
            showAlertDialog(getString(R.string.image_perm), "", "", getString(R.string.allow),
                DialogInterface.OnClickListener { _, which ->
                    when (which) {
                        DialogInterface.BUTTON_NEUTRAL -> ActivityCompat.requestPermissions(
                            activity,
                            requiredPermission.toTypedArray(),
                            REQ_IMAGE_PERM
                        )
                    }
                })
        } else {
            continueWithImageTasks()
        }
    }

    fun continueWithImageTasks() {
       /* CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .setActivityTitle(getString(R.string.crop_image))
            .setCropShape(CropImageView.CropShape.RECTANGLE)
            .setCropMenuCropButtonTitle(getString(R.string.done))
            .start(activity)*/
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQ_IMAGE_PERM) {
            val permissionResults = HashMap<String, Int>()
            var deniedCount = 0

            /** Add all denied permissions  */
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults[permissions[i]] = grantResults[i]
                    deniedCount++
                }
            }

            /** all permissions granted  */
            if (deniedCount == 0) {
                continueWithImageTasks()
            } else {
                for ((permName, permResult) in permissionResults) {
                    /** permission denied (first time, when 'Dont ask again' has not been checked).
                     * ask again, explaining the requirement
                     * shouldShowRequestPermissionRationale wil return true
                     */
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permName)) {
                        showAlertDialog(getString(R.string.image_perm), "", "", getString(R.string.allow),
                            DialogInterface.OnClickListener { _, which ->
                                when (which) {
                                    DialogInterface.BUTTON_NEUTRAL -> verifyImagePermissions()
                                }
                            })
                    }
                    /** Permission has been denied and Dont ask again checked shouldShowrequestPermissionRationale will return false **/
                    else {
                        showAlertDialog(getString(R.string.permission_alert), "", "", getString(R.string.ok),
                            DialogInterface.OnClickListener { _, which ->
                                when (which) {
                                    DialogInterface.BUTTON_NEUTRAL -> {
                                        val _intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package",activity.packageName, null)
                                        )
                                        _intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        startActivity(_intent)
                                        activity.finish()
                                    }
                                }
                            })
                    }
                }
            }
        }
    }

    open fun handleFinalCroppedImage(imageUri: Uri) {}

    /*
     * Begins to upload the file specified by the file path to AWS
     */

    open fun fileUploadSuccess(imageKey: String) {}

    open fun fileUploadError(){}

    fun showToast(msg : String){
        Toast.makeText(activity,msg,Toast.LENGTH_SHORT).show()
    }

    fun replaceFragment(container: Int, fragment: Fragment, enab_backStack: Boolean) {
        var fragmentTransaction = activity.supportFragmentManager.beginTransaction().replace(container, fragment)
        if (enab_backStack) {
            fragmentTransaction.addToBackStack(null)
        }
        fragmentTransaction.commit()
    }

    fun addFragment(container: Int, fragment: Fragment, enab_backStack: Boolean) {
        var fragmentTransaction = activity.supportFragmentManager.beginTransaction().add(container, fragment)
        if (enab_backStack) {
            fragmentTransaction.addToBackStack(null)
        }
        fragmentTransaction.commit()
    }


    open fun openUpQuestionaireScreen(){

    }

    fun setSpannable(
        textView: TextView,
        message: String,
        matched_string: String,
        color: Int,
        underlineStatus: Boolean /*, setOnClickListener: OnClickListener*/
    ) {

        var spannableStr = SpannableStringBuilder(message)


        var start = message.indexOf(matched_string, 1)
        spannableStr.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            start + matched_string.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val clickableSpan = object : ClickableSpan() {

            override fun onClick(textView: View) {
                // Toast.makeText(textView.context,"clicked",Toast.LENGTH_LONG).show()
                //  setOnClickListener.onClick(textView)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = underlineStatus
            }
        }

        spannableStr.setSpan(clickableSpan, start, start + matched_string.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)



        textView.setLinkTextColor(color)
        textView.setText(spannableStr, TextView.BufferType.SPANNABLE)
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = ContextCompat.getColor(textView.context, android.R.color.transparent)
    }

    fun getRangedArray(from : Int ,to : Int , plusOneMore : Boolean) : ArrayList<String>{
        var rangList : ArrayList<String> = ArrayList()
        for (i in from..to){
            rangList.add(""+i);
            if (i==to && plusOneMore)
                rangList.add(""+i+"+")
        }
        return rangList
    }


}

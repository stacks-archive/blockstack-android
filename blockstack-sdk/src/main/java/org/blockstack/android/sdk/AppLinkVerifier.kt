package org.blockstack.android.sdk

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.text.TextUtils
import android.util.Log
import androidx.annotation.WorkerThread
import org.blockstack.android.sdk.model.BlockstackConfig
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.URL
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Object to check Verified App links (@see https://developer.android.com/training/app-links/verify-site-associations).
 * Verified App Links is the recommended way for Android Blockstack apps to handle the redirect after login.
 *
 * @param context an activity or the application context
 * @param config the blockstack configuration used for this app
 */
class AppLinkVerifier(private val context: Context, private val config: BlockstackConfig) {

    /**
     * Verifies that digital asset links file of the Blockstack configuration and the
     * applications's signature matches.
     * @return human readable message if app links couldn't be verified.
     * @return
     */
    @WorkerThread
    fun verify(): String? {
        try {
            val fingerprintsFromDALFile = getFingerprintsFromDigitalAssetLinksFile()
            val fingerprintsFromPackage = getFingerprintsFromPackage()
            var msg: String? = null
            if (fingerprintsFromDALFile.isEmpty()) {
                msg = "Digital Asset Links file for ${config.appDomain} does not contain a fingerprint for this app ${context.packageName}.\nPlease verify https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=${config.appDomain}&relation=delegate_permission/common.handle_all_urls"
            }
            if (fingerprintsFromPackage.isEmpty()) {
                val msgFromPackage = "This app ${context.packageName} does not contain a signature."
                if (msg != null) {
                    msg = msg + "\n" + msgFromPackage
                } else {
                    msg = msgFromPackage
                }
            }
            if (!fingerprintsFromDALFile.isEmpty() && !fingerprintsFromPackage.isEmpty() &&
                    !fingerprintsFromDALFile.containsOne(fingerprintsFromPackage) ) {
                msg = "Fingerprints for ${context.packageName} at ${config.appDomain} do not match.\nFingerprint(s) from Digital Asset Links file: ${fingerprintsFromDALFile.joinToString()}\nFingerprint from application signature   : ${fingerprintsFromPackage.joinToString()}\nPlease verify https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=${config.appDomain}&relation=delegate_permission/common.handle_all_urls"
            }
            if (msg != null) {
                Log.w(TAG, "Blockstack apps should use Verified App Links. Read more at  https://developer.android.com/training/app-links/verify-site-associations\nThis warning can be suppressed by setting BlockstackSession.doNotVerifyAppLinkConfiguration = true.\n$msg")
            }
            return msg
        } catch (e: Exception) {
            val msg = "Failed to check verified app links. ${e}"
            Log.w(TAG, msg, e)
            return msg
        }
    }

    private fun getFingerprintsFromDigitalAssetLinksFile(): ArrayList<String> {
        val fingerprints = arrayListOf<String>()

        val responseString = URL("https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=${config.appDomain}&relation=delegate_permission/common.handle_all_urls").readText()
        val normalizedAppDomain = "${config.appDomain.getOrigin()}."
        val response = JSONObject(responseString)
        val statements = response.optJSONArray("statements")
        if (statements != null) {
            var statement: JSONObject
            for (i in 0..statements.length() - 1) {
                statement = statements.getJSONObject(i)
                val site = statement.optJSONObject("source")?.optJSONObject("web")?.optString("site")
                val androidApp = statement.optJSONObject("target")?.optJSONObject("androidApp")
                if (normalizedAppDomain.equals(site) && context.packageName.equals(androidApp?.optString("packageName"))) {
                    val fingerprint = androidApp?.optJSONObject("certificate")?.optString("sha256Fingerprint")
                    if (fingerprint != null) {
                        fingerprints.add(fingerprint)
                    }
                }
            }
        }

        return fingerprints
    }

    @Suppress("DEPRECATION")
    private fun getFingerprintsFromPackage(): ArrayList<String> {
        val fingerprints = arrayListOf<String>()
        val pm = context.getPackageManager()
        val packageName = context.getPackageName()
        val flags = PackageManager.GET_SIGNATURES
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = pm.getPackageInfo(packageName, flags)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        val signatures = packageInfo!!.signatures
        for (signature in signatures) {
            val cert = signature.toByteArray()
            val input = ByteArrayInputStream(cert)
            var cf: CertificateFactory? = null
            try {
                cf = CertificateFactory.getInstance("X509")
            } catch (e: CertificateException) {
                e.printStackTrace()
            }

            var c: X509Certificate? = null
            try {
                c = cf!!.generateCertificate(input) as X509Certificate
            } catch (e: CertificateException) {
                e.printStackTrace()
            }

            try {
                val md = MessageDigest.getInstance("SHA256")
                val publicKey = md.digest(c!!.getEncoded())
                val hexString = byte2HexFormatted(publicKey)
                fingerprints.add(hexString)
            } catch (e1: NoSuchAlgorithmException) {
                e1.printStackTrace()
            } catch (e: CertificateEncodingException) {
                e.printStackTrace()
            }
        }
        return fingerprints
    }

    fun byte2HexFormatted(arr: ByteArray): String {
        val str = StringBuilder(arr.size * 2)
        for (i in arr.indices) {
            var h = Integer.toHexString(arr[i].toInt())
            val l = h.length
            if (l == 1) h = "0$h"
            if (l > 2) h = h.substring(l - 2, l)
            str.append(h.toUpperCase())
            if (i < arr.size - 1) str.append(':')
        }
        return str.toString()
    }

    companion object {
        private val TAG = AppLinkVerifier::class.java.simpleName
    }
}

private fun <E> java.util.ArrayList<E>.containsOne(list: java.util.ArrayList<E>): Boolean {
    for (element in this) {
        if (list.contains(element)) {
            return true
        }
    }
    return false
}

package org.blockstack.collection

import org.blockstack.android.sdk.optStringOrNull
import org.json.JSONObject


class Contact(attr: ContactAttr) : Collection<ContactAttr>(attr) {

    override val collectionName: String = Contact.collectionName

    companion object : CollectionAPI<ContactAttr> {
        override val fromData: (String) -> ContactAttr =
                { fileContent: String ->
                    JSONObject(fileContent).toContactAttr()
                }

        override val fromAttrs: (ContactAttr) -> Collection<ContactAttr> =
                { attrs: ContactAttr ->
                    Contact(attrs)
                }

        override val collectionName: String
            get() = "Contact"
    }
}

data class ContactAttr(val schemaVersion: String, override val identifier: String, val firstName: String, val lastName: String,
                       val blockstackID: String?,
                       val email: String?, val website: String?, val address: String?, val telephone: String?,
                       val organization: String?,
                       override val createdAt: Number?, override val updatedAt: Number?,
                       override val signingKeyId: String?, override val _id: String?) : Attrs {

    override fun toJSON(): JSONObject {
        return super.toJSON()
                .put("schemaVersion", schemaVersion)
                .put("firstName", firstName)
                .put("lastName", lastName)
                .put("blockstackID", blockstackID)
                .put("email", email)
                .put("website", website)
                .put("address", address)
                .put("telephone", telephone)
                .put("organization", organization)
    }
}

private fun JSONObject.toContactAttr(): ContactAttr {
    return ContactAttr("1.0", this.getString("identifier"),
            this.getString("firstName"), this.getString("lastName"),
            this.optStringOrNull("blockstackID"),
            this.optStringOrNull("email"),
            this.optStringOrNull("website"),
            this.optStringOrNull("address"),
            this.optStringOrNull("telephone"),
            this.optStringOrNull("organization"),
            this.optInt("createdAt"),
            this.optInt("updatedAt"),
            this.optStringOrNull("signingKeyId"),
            this.optStringOrNull("_id")
    )
}
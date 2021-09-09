package org.blockstack.android.sdk.model

import org.blockstack.android.sdk.extensions.toHexPublicKey64
import org.kethereum.bip32.model.ExtendedKey
import org.komputing.khash.sha256.Sha256
import org.komputing.khex.extensions.toNoPrefixHexString

data class BlockstackIdentity(val identityKeys: ExtendedKey) {

    var salt: String
        private set

    init {
        salt = Sha256.digest(identityKeys.keyPair.toHexPublicKey64().toByteArray()).toNoPrefixHexString()
    }

}
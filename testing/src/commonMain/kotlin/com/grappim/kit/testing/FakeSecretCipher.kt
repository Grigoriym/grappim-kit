package com.grappim.kit.testing

import com.grappim.kit.storage.SecretCipher

class FakeSecretCipher : SecretCipher {

    /** Overrides the normal `"ENC:"`-strip decrypt, e.g. to simulate a lost key by returning `null`. */
    var decryptResult: ((String) -> String?)? = null

    override fun encrypt(value: String): String = "ENC:$value"

    override fun decrypt(value: String): String? {
        decryptResult?.let { return it(value) }
        return value.removePrefix("ENC:")
    }
}

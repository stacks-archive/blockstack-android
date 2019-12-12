package org.blockstack.android.sdk

import org.blockstack.android.sdk.model.ZoneFile
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Integer.parseInt
import java.util.*


data class URIType(val name: String, val target: String, val priority: Int, val weight: Int, val ttl: Int?)

fun parseZoneFile(text: String): ZoneFile {
    var normalizedText = removeComments(text)
    //normalizedText = flatten(normalizedText)
    return parseRRs(normalizedText)
}

fun removeComments(text: String): String {
    val re = Regex(pattern = "^.*")
    return text.replace(re, "")
}

fun flatten(text: String): String {
    val re = Regex("([\\s\\S]+?)")
    return text.replace(re, " ")
}

fun parseRRs(text: String): ZoneFile {
    val ret = JSONObject()
            .put("txt", JSONArray())
            .put("ns", JSONArray())
            .put("a", JSONArray())
            .put("aaaa", JSONArray())
            .put("cname", JSONArray())
            .put("mx", JSONArray())
            .put("ptr", JSONArray())
            .put("srv", JSONArray())
            .put("spf", JSONArray())
            .put("uri", JSONArray())

    val rrs = text.split("\n")
    for (rr in rrs) {
        if (rr.trim().isEmpty()) {
            continue
        }
        val uRR = rr.toUpperCase(Locale.US)
        /*
        if (Regex("\s+TXT\s+/").matches(uRR)) {
            ret.getJSONArray("txt").put(parseTXT(rr))
        } else */ if (uRR.indexOf("\$ORIGIN") == 0) {
            ret.put("\$origin", rr.split(Regex("\\s+"))[1])
        } else if (uRR.indexOf("\$TTL") == 0) {
            ret.put("ttl", parseInt(rr.split(Regex("\\s+"))[1], 10))
        } else /*if (Regex("\s+SOA\s+").matches(uRR)) {
            ret.put("soa", parseSOA(rr))
        } else if (Regex("\s+NS\s+").matches(uRR)) {
            ret.getJSONArray("ns").put(parseNS(rr))
        } else if (Regex("\s+A\s+").matches(uRR)) {
            ret.getJSONArray("a").put(parseA(rr, ret.getJSONArray("a")))
        } else if (Regex("\s+AAAA\s+").matches(uRR)) {
            ret.getJSONArray("aaaa").put(parseAAAA(rr))
        } else if (Regex("\s+CNAME\s+").matches(uRR)) {
            ret.getJSONArray("cname").put(parseCNAME(rr))
        } else if (Regex("\s+MX\s+").matches(uRR)) {
            ret.getJSONArray("mx").put(parseMX(rr))
        } else if (Regex("\s+PTR\s+").matches(uRR)) {
            ret.getJSONArray("ptr").put(parsePTR(rr, ret.getJSONArray("ptr"), ret.getString("\$origin")))
        } else if (Regex("\s+SRV\s+").matches(uRR)) {
            ret.getJSONArray("srv").put(parseSRV(rr))
        } else if (Regex("\s+SPF\s+").matches(uRR)) {
            ret.getJSONArray("spf").put(parseSPF(rr))
        } else */if (Regex("\\sURI\\s").find(uRR) != null) {
            ret.getJSONArray("uri").put(parseURI(rr))
        }
    }
    return ZoneFile(ret)
}

/*
fun parseSOA(rr:String) {
    val soa = {}
    val rrTokens = rr.trim().split(/\s+/g)
    val l = rrTokens.length
            soa.name = rrTokens[0]
    soa.minimum = parseInt(rrTokens[l - 1], 10)
    soa.expire = parseInt(rrTokens[l - 2], 10)
    soa.retry = parseInt(rrTokens[l - 3], 10)
    soa.refresh = parseInt(rrTokens[l - 4], 10)
    soa.serial = parseInt(rrTokens[l - 5], 10)
    soa.rname = rrTokens[l - 6]
    soa.mname = rrTokens[l - 7]
    if (!isNaN(rrTokens[1])) soa.ttl = parseInt(rrTokens[1], 10)
    return soa
}

fun parseNS(rr:String) {
    val rrTokens = rr.trim().split(/\s+/g)
    val l = rrTokens.length
            val result: NSType = {
        name: rrTokens[0],
        host: rrTokens[l - 1]
    }

    if (!isNaN(rrTokens[1])) result.ttl = parseInt(rrTokens[1], 10)
    return result
}

fun parseA(rr:String, recordsSoFar:JSONArray) {
    val rrTokens = rr.trim().split(/\s+/g)
    val urrTokens = rr.trim().toUpperCase().split(/\s+/g)
    val l = rrTokens.length
            val result: AType = {
        name: rrTokens[0],
        ip: rrTokens[l - 1]
    }

    if (urrTokens.lastIndexOf('A') === 0) {
        if (recordsSoFar.length) {
            result.name = recordsSoFar[recordsSoFar.length - 1].name
        } else {
            result.name = '@'
        }
    }

    if (!isNaN(rrTokens[1])) result.ttl = parseInt(rrTokens[1], 10)
    return result
}

fun parseAAAA(rr:String) {
    val rrTokens = rr.trim().split(/\s+/g)
    val l = rrTokens.length
            val result: AType = {
        name: rrTokens[0],
        ip: rrTokens[l - 1]
    }

    if (!isNaN(rrTokens[1])) result.ttl = parseInt(rrTokens[1], 10)
    return result
}

fun parseCNAME(rr:String) {
    val rrTokens = rr.trim().split(/\s+/g)
    val l = rrTokens.length
            val result: CNAMEType = {
        name: rrTokens[0],
        alias: rrTokens[l - 1]
    }

    if (!isNaN(rrTokens[1])) result.ttl = parseInt(rrTokens[1], 10)
    return result
}

fun parseMX(rr:String) {
    val rrTokens = rr.trim().split(/\s+/g)
    val l = rrTokens.length
            val result: MXType = {
        name: rrTokens[0],
        preference: parseInt(rrTokens[l - 2], 10),
        host: rrTokens[l - 1]
    }

    if (!isNaN(rrTokens[1])) result.ttl = parseInt(rrTokens[1], 10)
    return result
}

fun parseTXT(rr:String) {
    val rrTokens = rr.trim().match(/[^\s\"']+|\"[^\"]*\"|'[^']*'/g)
    if (!rrTokens)
        throw new Error('Failure to tokenize TXT record')
    val l = rrTokens.length
            val indexTXT = rrTokens.indexOf('TXT')

    fun stripText(txt) {
        if (txt.indexOf('\"') > -1) {
            txt = txt.split('\"')[1]
        }
        if (txt.indexOf('"') > -1) {
            txt = txt.split('"')[1]
        }
        return txt
    }

    let tokenTxt: string|Array<string>
    if (l - indexTXT - 1 > 1) {
        tokenTxt = rrTokens
                .slice(indexTXT + 1)
                .map(stripText)
    } else {
        tokenTxt = stripText(rrTokens[l - 1])
    }

    val result: TXTType = {
        name: rrTokens[0],
        txt: tokenTxt
    }

    if (!isNaN(rrTokens[1])) result.ttl = parseInt(rrTokens[1], 10)
    return result
}

fun parsePTR(rr:String, recordsSoFar:JSONArray, currentOrigin:String) {
    val rrTokens = rr.trim().split(/\s+/g)
    val urrTokens = rr.trim().toUpperCase().split(/\s+/g)

    if (urrTokens.lastIndexOf('PTR') === 0 && recordsSoFar[recordsSoFar.length - 1]) {
        rrTokens.unshift(recordsSoFar[recordsSoFar.length - 1].name)
    }

    val l = rrTokens.length
            val result: NSType = {
        name: rrTokens[0],
        fullname: rrTokens[0] + '.' + currentOrigin,
        host: rrTokens[l - 1]
    }

    if (!isNaN(rrTokens[1])) result.ttl = parseInt(rrTokens[1], 10)
    return result
}

fun parseSRV(rr:String) {
    val rrTokens = rr.trim().split(/\s+/g)
    val l = rrTokens.length
            val result: SRVType = {
        name: rrTokens[0],
        target: rrTokens[l - 1],
        priority: parseInt(rrTokens[l - 4], 10),
        weight: parseInt(rrTokens[l - 3], 10),
        port: parseInt(rrTokens[l - 2], 10)
    }

    if (!isNaN(rrTokens[1])) result.ttl = parseInt(rrTokens[1], 10)
    return result
}

fun parseSPF(rr:String) {
    val rrTokens = rr.trim().split(/\s+/g)
    val result: SPFType = {
        name: rrTokens[0],
        data: ''
    }

    let l = rrTokens.length
            while (l-- > 4) {
                result.data = rrTokens[l] + ' ' + result.data.trim()
            }

    if (!isNaN(rrTokens[1])) result.ttl = parseInt(rrTokens[1], 10)
    return result
}

 */

fun parseURI(rr: String): URIType {
    val rrTokens = rr.trim().split(Regex("\\s+"))
    val l = rrTokens.size

    return URIType(
            name = rrTokens[0],
            target = rrTokens[l - 1].replace("\"", ""),
            priority = parseInt(rrTokens[l - 3], 10),
            weight = parseInt(rrTokens[l - 2], 10),
            ttl = try {
                parseInt(rrTokens[1], 10)
            } catch (e: NumberFormatException) {
                null
            }
    )
}


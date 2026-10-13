package ai.flux.mobile.audio

/** Stateless meter: PCM samples are not retained, logged or persisted. */
internal fun fluxMicLevel(bytes:ByteArray):Float {
    if(bytes.size<2)return 0f
    var sum=0.0;var count=0
    for(i in 0 until bytes.size-1 step 2){val sample=((bytes[i].toInt() and 255) or (bytes[i+1].toInt() shl 8)).toShort().toDouble()/32768.0;sum+=sample*sample;count++}
    return (kotlin.math.sqrt(sum/count)*8).toFloat().coerceIn(0f,1f)
}

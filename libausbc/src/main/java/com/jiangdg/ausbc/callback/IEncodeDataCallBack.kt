package com.jiangdg.ausbc.callback

/**
 * Encode data callback
 *
 * @author Created by jiangdg on 2022/1/29
 */
interface IEncodeDataCallBack {
    fun onEncodeData(data: ByteArray?, size: Int, type: DataType, timestamp: Long)
    enum class DataType {
        AAC,       // aac with ADTS
        H264_KEY,  // H.264, key frame
        H264_SPS,  // H.264, sps & pps
        H264       // H.264 not key frame
    }
}
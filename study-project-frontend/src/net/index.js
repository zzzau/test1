import axios from 'axios'
import { ElMessage } from 'element-plus'

const defaultFailure = (message) => ElMessage.warning(message)

const defaultError = (error) => {
    const message =
        error.response?.data?.message ||
        error.response?.data?.data ||
        '发生了一些错误，请联系管理员'

    ElMessage.error(message)
}

function post(url, data, success, failure = defaultFailure, error = defaultError) {
    axios.post(url, new URLSearchParams(data), {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        withCredentials: true
    }).then(({ data }) => {

        console.log('后端返回：', data)

        const isSuccess =
            data.success === true ||
            data.status === 200 ||
            data.code === 200

        const message =
            data.message ||
            data.data ||
            '操作成功'

        if (isSuccess) {
            success(message, data.status || data.code)
        } else {
            failure(message || '请求失败', data.status || data.code)
        }
    }).catch(error)
}

function get(url, success, failure = defaultFailure, error = defaultError) {
    axios.get(url, {
        withCredentials: true
    }).then(({ data }) => {

        console.log('后端返回：', data)

        const isSuccess =
            data.success === true ||
            data.status === 200 ||
            data.code === 200

        const message =
            data.message ||
            data.data ||
            '操作成功'

        if (isSuccess) {
            success(message, data.status || data.code)
        } else {
            failure(message || '请求失败', data.status || data.code)
        }
    }).catch(error)
}

export { get, post }

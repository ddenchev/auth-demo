package doichin.auth.client

import doichin.auth.dto.App
import doichin.auth.dto.CreateAppRequest
import doichin.auth.dto.Listing
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import kotlin.uuid.Uuid


suspend fun AuthClient.createApp(req: CreateAppRequest): ApiResponse<App> {
    return post<App>("${authUrl}/apps") {
        setBody(req)
    }
}

suspend fun AuthClient.getApp(appId: Uuid): ApiResponse<App> {
    return get("${authUrl}/apps/${appId}")
}


suspend fun AuthClient.listApps(offset: Int = 0, limit: Int = 20): ApiResponse<Listing<App>> {
    return get("${authUrl}/apps") {
        parameter("offset", offset)
        parameter("limit", limit)
    }
}


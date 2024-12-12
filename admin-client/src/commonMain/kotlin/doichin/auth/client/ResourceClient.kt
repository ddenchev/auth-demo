package doichin.auth.client

import doichin.auth.dto.AllocateResourceRequest
import doichin.auth.dto.CreateResourceRequest
import doichin.auth.dto.Listing
import doichin.auth.dto.Resource
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import kotlin.uuid.Uuid

suspend fun AuthClient.createResource(req: CreateResourceRequest): ApiResponse<Resource> {
    return post<Resource>("${authUrl}/resources") {
        setBody(req)
    }
}

suspend fun AuthClient.allocateResource(req: AllocateResourceRequest): ApiResponse<List<Resource>> {
    return post<List<Resource>>("${authUrl}/resources/allocate") {
        setBody(req)
    }
}

suspend fun AuthClient.getResource(id: Uuid): ApiResponse<Resource> {
    return get("${authUrl}/resources/${id}")
}


suspend fun AuthClient.listResources(offset: Int = 0, limit: Int = 20): ApiResponse<Listing<Resource>> {
    return get("${authUrl}/resources") {
        parameter("offset", offset)
        parameter("limit", limit)
    }
}


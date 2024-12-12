package doichin.auth.lib

sealed interface ServiceException

class AuthorizationException(override val message: String? = null) : Throwable(), ServiceException
class NotFoundException(override val message: String? = null) : Throwable(), ServiceException
class ValidationException(override val message: String? = null) : Throwable(), ServiceException
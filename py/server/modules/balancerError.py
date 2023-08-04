class BalancerError(object):
    error_dict = {
        101: "Can not add ownership",
        102: "Could not get ownership for given sc address",
        103: "Could not get owner sc addresses",
        104: "Can not create proposal",
        105: "Can not get active proposal",
        106: "Generic error"
    }

    def __init__(self, code, detail=""):
        self.code = code
        if code in self.error_dict:
            self.description = self.error_dict[code]
        else:
            self.description = "Invalid error code: " + code + ". Pls add it to the catalog"
        self.detail = detail

    def get(self):
        return {
            "error": {
                "code": self.code,
                "description": self.description,
                "detail": self.detail
            }
        }


# specific classes helping in getting the right error code
class AddOwnershipError(BalancerError):
    def __init__(self, detail=""):
        super().__init__(101, detail)


class GetOwnershipError(BalancerError):
    def __init__(self, detail=""):
        super().__init__(102, detail)


class GetOwnerScAddressesError(BalancerError):
    def __init__(self, detail=""):
        super().__init__(103, detail)


class CreateProposalError(BalancerError):
    def __init__(self, detail=""):
        super().__init__(104, detail)


class GetProposalError(BalancerError):
    def __init__(self, detail=""):
        super().__init__(105, detail)


class GenericError(BalancerError):
    def __init__(self, detail=""):
        super().__init__(106, detail)

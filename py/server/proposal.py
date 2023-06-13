import json

class VotingProposal(object):
    def __init__(self, id, bl_height, bl_hash, from_time, to_time, author):
        self.id = id
        self.block_height = bl_height
        self.block_hash = bl_hash
        self.fromTime = from_time
        self.toTime = to_time
        self.author = author

    def toJSON(self):
        return {
            "Proposal": {
                "ID": self.id,
                "block_height": self.block_height,
                "block_hash": self.block_hash,
                "from": str(self.fromTime),
                "to": str(self.toTime),
                "Author": self.author
            }
        }

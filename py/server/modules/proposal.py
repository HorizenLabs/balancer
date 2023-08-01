class VotingProposal(object):
    def __init__(self, in_id, bl_height=0, bl_hash="", from_time=None, to_time=None, author=""):
        self.id = in_id
        self.block_height = bl_height
        self.block_hash = bl_hash
        self.fromTime = from_time
        self.toTime = to_timeh
        self.author = author

    def to_json(self):
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

    def is_null(self):
        return self.id is None

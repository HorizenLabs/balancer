class VotingProposal(object):
    def __init__(self, in_id, bl_height=0, bl_hash="", from_time=None, to_time=None, author="", snapshot=0):
        self.id = in_id
        self.mc_block_height = bl_height
        self.mc_block_hash = bl_hash
        self.fromTime = from_time
        self.toTime = to_time
        self.author = author
        self.snapshot=snapshot

    def to_json(self):
        return {
            "Proposal": {
                "ID": self.id,
                "block_height": self.mc_block_height,
                "block_hash": self.mc_block_hash,
                "from": str(self.fromTime),
                "to": str(self.toTime),
                "Author": self.author,
                "snapshot":self.snapshot
            }
        }

    def is_null(self):
        return self.id is None

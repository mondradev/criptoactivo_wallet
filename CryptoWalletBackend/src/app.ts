import express from "express"
import Bitcoin from "./libs/bitcoin";

Bitcoin.Blockchain.sync()

const app = express()

export default app
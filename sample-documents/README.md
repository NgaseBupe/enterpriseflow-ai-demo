# Sample documents

Synthetic purchase orders for trying the app. **All names, addresses, contact details and amounts are fictitious.**

| File | Type | Customer |
|---|---|---|
| `po-1001.pdf` | PDF | Chanda Hardware Ltd |
| `po-1002.pdf` | PDF | Mwila Building Supplies |
| `po-1003-scan.png` | Image (a "scanned" order) | Kasonde Engineering |

The app's default AI provider is a built-in **mock**, which returns a fixed purchase order instead of reading the file. These samples are printed with exactly the data the mock returns for them, so the extracted values match the document on screen. `SampleDocumentsTest` keeps the two in sync.

With a real AI provider, any purchase order can be used.

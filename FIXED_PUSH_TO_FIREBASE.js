/**
 * ============================================
 * FIXED task_pushDataToFirebase() FUNCTION
 * ============================================
 * 
 * This fixes the "could not deserialize object" error
 * by ensuring proper data type conversion
 * 
 * REPLACE your existing task_pushDataToFirebase() function with this
 */

function task_pushDataToFirebase() {
  const props = PropertiesService.getScriptProperties();
  const token = getServiceAccountToken();
  const sheet = getSheet();
  const data = sheet.getDataRange().getValues();
  
  if (data.length <= 1) return { success: true, message: "No data", total: 0 };

  const headers = data[0].map(h => String(h).trim());
  const kgidIdx = headers.indexOf("kgid");
  if (kgidIdx < 0) throw new Error("kgid column required");

  const totalRows = data.length - 1;
  props.setProperty("S2F_TOTAL", String(totalRows));
  let uploaded = 0;
  let errors = 0;

  // Define boolean field names (case-insensitive check)
  const booleanFields = ["isDeleted", "isApproved", "isActive", "isVerified", "isBlocked"];
  
  // Define numeric field names (if you have specific numeric fields)
  const numericFields = ["mobile1", "mobile2", "landline", "pincode", "age", "experience"];

  for (let r = 1; r < data.length; r++) {
    const row = data[r];
    const kgid = String(row[kgidIdx] || "").trim();

    props.setProperty("S2F_CURRENT_ROW", String(r + 1));
    props.setProperty("S2F_CURRENT_KGID", kgid);

    if (!kgid) continue;

    const fields = {};

    headers.forEach((h, idx) => {
      if (h === 'kgid') return; // Skip kgid, it's the document ID
      
      let val = row[idx];
      
      // Skip null, undefined, and empty strings
      if (val === null || val === undefined || val === "") {
        return; // Don't store empty/null values
      }

      const fieldNameLower = h.toLowerCase();
      const isBooleanField = booleanFields.some(bf => fieldNameLower === bf.toLowerCase());
      const isNumericField = numericFields.some(nf => fieldNameLower === nf.toLowerCase());

      // Handle boolean fields
      if (isBooleanField) {
        // Convert string "true"/"false" to boolean
        if (typeof val === "string") {
          const strVal = val.trim().toLowerCase();
          fields[h] = { booleanValue: (strVal === "true" || strVal === "1" || strVal === "yes") };
        } else if (typeof val === "boolean") {
          fields[h] = { booleanValue: val };
        } else if (typeof val === "number") {
          fields[h] = { booleanValue: val !== 0 };
        } else {
          // Default to false for unknown types
          fields[h] = { booleanValue: false };
        }
      }
      // Handle numeric fields
      else if (isNumericField || typeof val === "number") {
        if (typeof val === "string") {
          // Try to parse string as number
          const numVal = parseFloat(val);
          if (!isNaN(numVal)) {
            if (Number.isInteger(numVal)) {
              fields[h] = { integerValue: String(Math.floor(numVal)) };
            } else {
              fields[h] = { doubleValue: numVal };
            }
          } else {
            // If can't parse as number, store as string
            fields[h] = { stringValue: String(val) };
          }
        } else if (typeof val === "number") {
          if (Number.isInteger(val)) {
            fields[h] = { integerValue: String(val) };
          } else {
            fields[h] = { doubleValue: val };
          }
        } else {
          fields[h] = { stringValue: String(val) };
        }
      }
      // Handle Date objects
      else if (val instanceof Date) {
        fields[h] = { timestampValue: val.toISOString() };
      }
      // Handle boolean (non-boolean field but value is boolean)
      else if (typeof val === "boolean") {
        fields[h] = { booleanValue: val };
      }
      // Handle strings (default)
      else {
        // Trim and store as string
        const strVal = String(val).trim();
        if (strVal !== "") {
          fields[h] = { stringValue: strVal };
        }
      }
    });

    // Always add updatedAt timestamp
    fields.updatedAt = { timestampValue: new Date().toISOString() };

    const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees/${encodeURIComponent(kgid)}?currentDocument.exists=true`;

    try {
      const resp = UrlFetchApp.fetch(url, { 
        method: "PATCH", 
        contentType: "application/json", 
        headers: { Authorization: "Bearer " + token }, 
        payload: JSON.stringify({ fields: fields }), 
        muteHttpExceptions: true 
      });

      const code = resp.getResponseCode();
      
      if (code === 200) {
        uploaded++;
      } else if (code === 404) {
        // Document doesn't exist, create it
        const createUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees?documentId=${encodeURIComponent(kgid)}`;
        const createResp = UrlFetchApp.fetch(createUrl, {
          method: "POST",
          contentType: "application/json",
          headers: { Authorization: "Bearer " + token },
          payload: JSON.stringify({ fields: fields }),
          muteHttpExceptions: true
        });
        
        if (createResp.getResponseCode() === 200 || createResp.getResponseCode() === 201) {
          uploaded++;
        } else {
          Logger.log(`Failed to create ${kgid}: ${createResp.getContentText()}`);
          errors++;
        }
      } else {
        Logger.log(`Failed to update ${kgid} (code ${code}): ${resp.getContentText()}`);
        errors++;
      }
    } catch (e) {
      Logger.log(`Error processing row ${r + 1} (${kgid}): ${e.toString()}`);
      errors++;
    }

    props.setProperty("S2F_UP_COUNT", String(uploaded));
  }

  props.deleteProperty("S2F_CURRENT_KGID");
  props.deleteProperty("S2F_CURRENT_ROW");

  const res = { 
    success: true, 
    message: `Pushed ${uploaded}/${totalRows}${errors > 0 ? ` (${errors} errors)` : ""}`, 
    total: totalRows, 
    done: uploaded,
    errors: errors
  };

  writeLog("PUSH", res);
  return res;
}









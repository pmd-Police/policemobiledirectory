/**
 * === USEFUL LINKS SCRIPT - API HANDLERS ===
 * Handles external API requests (doGet)
 */

// =======================================================
// GET Handler (for external API use)
// =======================================================
function doGet() {
  const sheet = getUsefulLinksSheet();
  const data = sheet.getDataRange().getValues();
  const result = [];
  
  for (let i = 1; i < data.length; i++) {
    if (!data[i][0]) continue;
    result.push({
      name: data[i][0],
      playStoreUrl: data[i][1],
      apkUrl: data[i][4],
      category: data[i][3],
    });
  }
  
  return ContentService
    .createTextOutput(JSON.stringify(result))
    .setMimeType(ContentService.MimeType.JSON);
}


package com.example.policemobiledirectory.utils

object Constants {

    // Version number for constants - increment when constants structure changes
    const val LOCAL_CONSTANTS_VERSION = 2

    // Updated list of all ranks in the desired order for dropdowns
    val allRanksList = listOf(
        "APC", "CPC", "WPC", "PCW", "PC", "AHC", "CHC", "WHC", "HCW", "HC",
        "ASI", "ARSI", "WASI", "ASIW", "RSI", "PSI", "WPSI", "PSIW",
        "RPI", "CPI", "PI", "PIW", "WPI", "DYSP", "SDA", "FDA", "SS",
        "GHA", "AO", "Typist", "Steno", "PA"
    ).sorted()

    // Set of ranks that require a metal number
    val ranksRequiringMetalNumber = setOf(
        "APC", "CPC", "WPC", "PC", "AHC", "CHC", "WHC", "HC"
    ).sorted()

    val bloodGroupsList = listOf(
        "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "??"
    ).sorted()

    val districtsList = listOf(
        "Bagalkot", "Ballari", "Belagavi City", "Belagavi Dist", "Bengaluru City", "Bengaluru Dist", "Bidar",
        "Chamarajanagar", "Chikkaballapura", "Chikkamagaluru", "Chitradurga",
        "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Hassan", "Haveri",
        "Hubballi Dharwad City", "K.G.F", "Kalaburagi", "Kalaburagi City", "Kodagu", "Kolar", "Koppal", "Mandya",
        "Mangaluru City", "Mysuru City", "Mysuru Dist",
        "Raichur", "Ramanagara", "Shivamogga", "Tumakuru", "Udupi", "Uttara Kannada",
        "Vijayanagara", "Vijayapur", "Yadgir"
    ).sorted()

    // This map contains station lists for ALL districts
    // All stations (including common units) are hardcoded in each district's list
    val stationsByDistrictMap: Map<String, List<String>> = districtsList.associateWith { districtName ->
        val specificStations = when (districtName) {
            "Bagalkot" -> listOf(
                "Amengad PS", "Badami PS", "Bagalkot CEN Crime PS", "Bagalkot Rural PS",
                "Bagalkot Town PS", "Bagalkot Traffic PS", "Bagalkot Women PS", "Banahatti PS",
                "Bilagi PS", "Guledagudda PS", "Hungunda PS", "Ilakal PS", "Ilakal Rural PS",
                "Jamakhandi Rural PS", "Jamakhandi Town PS", "Kaladagi PS", "Kerur PS",
                "Lokapur PS", "Mahalingapur PS", "Mudhol PS", "Navanagara PS", "Savalgi PS",
                "Teradal PS",
                "Control Room Bagalkot", "DPO Bagalkot", "Computer Sec Bagalkot", "DAR Bagalkot",
                "FPB Bagalkot", "MCU Bagalkot", "DCRB Bagalkot", "DSB Bagalkot", "SMMC Bagalkot",
                "State INT Bagalkot", "DCRE Bagalkot", "Lokayukta Bagalkot", "ESCOM Bagalkot"
            )
            "Ballari" -> listOf(
                "APMC Yard PS", "Ballari CEN Crime PS", "Ballari Women PS", "Bellary Rural PS",
                "Bellary Traffic PS", "Brucepet PS", "Choranuru PS", "Cowlbazar PS",
                "Gandhinagar PS", "Hatcholli PS", "Kampli PS", "Kuduthini PS", "Kurugod PS",
                "Moka PS", "P.D. Halli PS", "Sandur PS", "Sirigeri PS", "Siruguppa PS",
                "Tekkalkota PS", "Thoranagal PS",
                "Control Room Ballari", "DPO Ballari", "Computer Sec Ballari", "DAR Ballari",
                "FPB Ballari", "MCU Ballari", "DCRB Ballari", "DSB Ballari", "SMMC Ballari",
                "State INT Ballari", "DCRE Ballari", "Lokayukta Ballari", "ESCOM Ballari"
            )
            "Belagavi City" -> listOf(
                "APMC Yard PS", "Bagewadi PS", "Belagavi City CEN Crime PS", "Belagavi City Women PS", "Belagavi Rural PS", 
                "Belgaum North Traffic PS", "Belgaum South Traffic PS", "Camp PS", "Kakati PS", "Khadebazar PS", 
                "Malamaruthi PS", "Marihal PS", "Market PS", "Shahapura PS", "Tilakwadi PS", "Udyambag PS",
                "Control Room Belagavi City", "DPO Belagavi City", "Computer Sec Belagavi City", "DAR Belagavi City",
                "FPB Belagavi City", "MCU Belagavi City", "DCRB Belagavi City", "DSB Belagavi City", "SMMC Belagavi City",
                "State INT Belagavi City", "DCRE Belagavi City", "Lokayukta Belagavi City", "ESCOM Belagavi City"
            )
            "Belagavi Dist" -> listOf(
                "Ankalgi PS", "Ankali PS", "Athani PS", "Bailahongal PS", "Basaweshwar Chowk PS",
                "Belagavi CEN Crime PS", "Belagavi Women PS", "Chikkodi PS", "Chikkodi Town Traffic PS",
                "Doddawad PS", "Ghataprabha PS", "Gokak Rural PS", "Gokak Town PS", "Harugeri PS",
                "Hukkeri PS", "Kagwad PS", "Katkol PS", "Khadakalat PS", "Khanapur PS", "Kittur PS",
                "Kudachi PS", "Kulgod PS", "Mudalgi PS", "Murgod PS", "Nandagad PS", "Nesargi PS",
                "Nippani Rural PS", "Nippani Town PS", "Raibag PS", "Ramadurga PS", "Sadalaga PS",
                "Sankeshwar PS", "Soundatti PS", "Yamakanmardi PS",
                "Control Room Belagavi Dist", "DPO Belagavi Dist", "Computer Sec Belagavi Dist", "DAR Belagavi Dist",
                "FPB Belagavi Dist", "MCU Belagavi Dist", "DCRB Belagavi Dist", "DSB Belagavi Dist", "SMMC Belagavi Dist",
                "State INT Belagavi Dist", "DCRE Belagavi Dist", "Lokayukta Belagavi Dist", "ESCOM Belagavi Dist"
            )
            "Bengaluru City" -> listOf(
                "Adugodi PS", "Adugodi Traffic PS", "Airport Traffic PS", "Amruthahally PS", "Annapoorneshwari Nagar PS", 
                "Ashoknagar PS", "Bagalagunte PS", "Bagalur PS", "Banasawadi Traffic PS", "Banashankari PS", 
                "Banashankari Traffic PS", "Banaswadi PS", "Bandepalya PS", "Basavanagudi PS", "Basavanagudi Traffic PS", 
                "Basavanagudi Women PS", "Basaveshwara Nagar PS", "Beguru PS", "Bellanduru PS", "Bellanduru Traffic PS", 
                "Bharathi Nagar PS", "BIAL PS", "Bommanahalli PS", "Byadarahalli PS", "Byappanahalli PS", 
                "Byatarayanapura PS", "Byatrarayanapura Traffic PS", "Central CEN Crime PS", "Chamarajpet PS", 
                "Chandra Layout PS", "Channammanakere Achu Kattu PS", "Chickpet Traffic PS", "Chikkajala PS", 
                "Chikkajala Traffic PS", "City Market PS", "City Market Traffic PS", "Commercial Street PS", 
                "Cottonpet PS", "Cubbon Park PS", "Cubbonpark Traffic PS", "Cyber Crime Police Station", 
                "Devanahalli PS", "Devanahalli Traffic PS", "Devarajeevanahalli PS", "East CEN Crime PS", 
                "East Zone Women PS", "Electronic City PS", "Electronic City Traffic PS", "Gangammana Gudi PS", 
                "Girinagar PS", "Govindapura PS", "Govindaraja Nagar PS", "H.A.L. PS", "Halasur Gate Traffic PS", 
                "Halasur PS", "Halasur Traffic PS", "Halasurgate PS", "Hanumanthanagar PS", "Hebbal PS", 
                "Hebbal Traffic PS", "Hennur PS", "Hennur Traffic PS", "High Grounds PS", "High Grounds Traffic PS", 
                "HSR Lay Out Traffic PS", "HSR Layout PS", "Hulimavu PS", "Hulimavu Traffic PS", "Indiranagar PS", 
                "J.C. Nagar PS", "Jagajeevanram Nagar PS", "Jalahalli PS", "Jalahalli Traffic PS", "Jayanagar PS", 
                "Jayanagar Traffic PS", "Jayaprakash Nagar PS", "Jeevan Bheemanagar PS", 
                "Jeevan Bhima Nagar (Indiranagar) Traffic PS", "Jnanabharathi PS", "K G Halli Traffic PS", 
                "K.R. Puram PS", "K.R.Puram Traffic PS", "Kadugodi PS", "Kadugondana Halli PS", "Kalasipalya PS", 
                "Kamakshipalya PS", "Kamakshipalya Traffic PS", "Kempapura Agrahara PS", "Kempegowda Nagar PS", 
                "Kengeri PS", "Kengeri Traffic PS", "Kodigehalli PS", "Konanakunte PS", "Koramangala PS", 
                "Kothanur PS", "Kumaraswamy Layout PS", "Kumaraswamy Layout Traffic PS", "Madivala PS", 
                "Madivala Traffic PS", "Magadi Road PS", "Magadi Road Traffic PS", "Mahadevapura PS", 
                "Mahadevapura Traffic PS", "Mahalakshmipuram PS", "Malleshwaram PS", "Malleshwaram Traffic PS", 
                "Marathahalli PS", "Mico Layout PS", "Mico Layout Traffic PS", "Nandini Layout PS", "North CEN Crime PS", 
                "NorthEast CEN Crime PS", "Parappana Agrahara", "Peenya PS", "Peenya Traffic PS", "Pulakeshinagar PS", 
                "Pulakeshinagar Traffic PS", "Puttenahalli PS", "R.M.C. Yard PS", "R.T. Nagar PS", 
                "R.T.Nagar Traffic PS", "Rajagopal Nagar PS", "Rajajinagar PS", "Rajajinagar Traffic PS", 
                "Rajarajeshwari Nagar PS", "Ramamurthy Nagar PS", "S.J. Park PS", "Sadashivanagar PS", 
                "Sadashivanagar Traffic PS", "Sampangiramanagar PS", "Sampigehalli PS", "Sanjay Nagar PS", 
                "Seshadripuram PS", "Shankarpura PS", "Shivajinagar PS", "Shivajinagar Traffic PS", "Siddapura PS", 
                "Soladevanahalli PS", "South CEN Crime PS", "SouthEast CEN Crime PS", "Srirampura PS", 
                "Subramanyanagar PS", "Subramanyapura PS", "Suddaguntepalya PS", "Test PS City 1", "Test PS City 2", 
                "Thalaghattapura PS", "Thalaghattapura Traffic PS", "Thilaknagar PS", "Upparpet PS", 
                "Upparpet Traffic PS", "V V Puram Traffic PS", "Varthur PS", "Vidhana Soudha PS", 
                "Vidyaranyapura PS", "Vijayanagar PS", "Vijayanagar Traffic PS", "Vishveshwarapuram PS", 
                "Viveknagar PS", "Vyalikaval PS", "West CEN Crime PS", "Whitefield CEN Crime PS", "Whitefield PS", 
                "Whitefield Traffic PS", "Wilsongarden PS", "Wilsongarden Traffic PS", "Yelahanka New Town PS", 
                "Yelahanka PS", "Yelahanka Traffic PS", "Yeshwanthapura PS", "Yeshwanthapura Traffic PS",
                "Control Room Bengaluru City", "DPO Bengaluru City", "Computer Sec Bengaluru City", "DAR Bengaluru City",
                "FPB Bengaluru City", "MCU Bengaluru City", "DCRB Bengaluru City", "DSB Bengaluru City", "SMMC Bengaluru City",
                "State INT Bengaluru City", "DCRE Bengaluru City", "Lokayukta Bengaluru City", "ESCOM Bengaluru City"
            )
            "Bengaluru Dist" -> listOf(
                "Anekal PS", "Anugondanahalli PS", "Attibele PS", "Avalahally PS", "Bannerghatta PS",
                "Bengaluru CEN Crime PS", "Bengaluru Dist Women PS", "Chennarayapatana PS", "Dobbespet PS",
                "Doddaballapura Rural PS", "Doddaballapura Town PS", "Doddabelavangala PS", "Hebbagodi PS",
                "Hosahalli PS", "Hosakote PS", "Hosakote Traffic PS", "Jigani PS", "Madanayakanahally PS",
                "Nandagudi PS", "Nelamangala Rural PS", "Nelamangala Town PS", "Nelamangala Traffic PS",
                "Rajanukunte PS", "Sarjapura PS", "Sulibele PS", "Suryanagar PS", "Thirumalashettahalli PS",
                "Thyamagondlu PS", "Vijayapura PS", "Vishwanathapura PS",
                "Control Room Bengaluru Dist", "DPO Bengaluru Dist", "Computer Sec Bengaluru Dist", "DAR Bengaluru Dist",
                "FPB Bengaluru Dist", "MCU Bengaluru Dist", "DCRB Bengaluru Dist", "DSB Bengaluru Dist", "SMMC Bengaluru Dist",
                "State INT Bengaluru Dist", "DCRE Bengaluru Dist", "Lokayukta Bengaluru Dist", "ESCOM Bengaluru Dist"
            )
            "Bidar" -> listOf(
                "Aurad PS", "Bagdal PS", "Basava Kalyana Rural PS", "Basava Kalyana Town PS",
                "Basava Kalyana Traffic PS", "Bemalkhed PS", "Bhalki Rural PS", "Bhalki Town PS",
                "Bidar CEN Crime PS", "Bidar Rural PS", "Bidar Town PS", "Bidar Traffic PS", "Bidar Women PS",
                "Chintaki PS", "Chitaguppa PS", "Dhannura PS", "Gandhi Gunj PS", "Hallikhed PS", "Hokrana PS",
                "Hulsoor PS", "Humnabad PS", "Humnabad Traffic PS", "Janwada PS", "Kamalanagar PS",
                "Khataka Chincholi PS", "Kushnoor PS", "Manna Ekhelli PS", "Mannalli PS", "Manthala PS",
                "Market PS", "Mehakar PS", "Mudbi PS", "New Town PS", "Santhapur PS",
                "Control Room Bidar", "DPO Bidar", "Computer Sec Bidar", "DAR Bidar",
                "FPB Bidar", "MCU Bidar", "DCRB Bidar", "DSB Bidar", "SMMC Bidar",
                "State INT Bidar", "DCRE Bidar", "Lokayukta Bidar", "ESCOM Bidar"
            )
            "Chamarajanagar" -> listOf(
                "Begur PS", "Chamarajanagar CEN Crime PS", "Chamarajanagar East PS", "Chamarajanagar Rural PS",
                "Chamarajanagar Town PS", "Chamarajanagar Traffic PS", "Chamrajanagar Women ps", "Gundlupete PS",
                "Hanur PS", "Kollegala Rural PS", "Kollegala Town PS", "Kuderu PS", "M.M. Hills PS",
                "Mambali PS", "Ramapura PS", "Santemaralli PS", "Terakanambi PS", "Yelandur PS",
                "Control Room Chamarajanagar", "DPO Chamarajanagar", "Computer Sec Chamarajanagar", "DAR Chamarajanagar",
                "FPB Chamarajanagar", "MCU Chamarajanagar", "DCRB Chamarajanagar", "DSB Chamarajanagar", "SMMC Chamarajanagar",
                "State INT Chamarajanagar", "DCRE Chamarajanagar", "Lokayukta Chamarajanagar", "ESCOM Chamarajanagar"
            )
            "Chikkaballapura" -> listOf(
                "Bagepalli PS", "Batlahalli PS", "Chelur PS", "Chikkaballapura CEN Crime PS",
                "Chikkaballapura Rural PS", "Chikkaballapura Town PS", "Chikkaballapura Traffic PS",
                "Chikkaballapura Women PS", "Chintamani Rural PS", "Chintamani Town PS", "Dibburahalli PS",
                "Gowribidanur Rural PS", "Gowribidanur Town PS", "Gudibande PS", "Kencharlahalli PS",
                "Manchenalli PS", "Nandi Giridhama PS", "Patapalya PS", "Peresandra PS",
                "Shidlagatta Rural PS", "Shidlaghatta Town PS",
                "Control Room Chikkaballapura", "DPO Chikkaballapura", "Computer Sec Chikkaballapura", "DAR Chikkaballapura",
                "FPB Chikkaballapura", "MCU Chikkaballapura", "DCRB Chikkaballapura", "DSB Chikkaballapura", "SMMC Chikkaballapura",
                "State INT Chikkaballapura", "DCRE Chikkaballapura", "Lokayukta Chikkaballapura", "ESCOM Chikkaballapura"
            )
            "Chikkamagaluru" -> listOf(
                "Ajjampura PS", "Aldur PS", "Balehonnur PS", "Balur PS", "Banakal PS", "Basavanahalli PS",
                "Birur PS", "Chickmagalur CEN Crime PS", "Chickmagalur Rural PS", "Chickmagalur Town PS",
                "Chickmagalur Traffic PS", "Chickmagalur Women PS", "Gonibeedu PS", "Hariharapura PS",
                "Jayapura PS", "Kadur PS", "Kalasa PS", "Koppa PS", "Kuduremukh PS", "Lakkavalli PS",
                "Lingadahalli PS", "Mallanduru PS", "Mudigere PS", "N.R. Pura PS", "Panchanahalli PS",
                "Sakkaraya Patna PS", "Singatagere PS", "Sringeri PS", "Tarikere Town PS",
                "Test PS District 1", "Yagati PS",
                "Control Room Chikkamagaluru", "DPO Chikkamagaluru", "Computer Sec Chikkamagaluru", "DAR Chikkamagaluru",
                "FPB Chikkamagaluru", "MCU Chikkamagaluru", "DCRB Chikkamagaluru", "DSB Chikkamagaluru", "SMMC Chikkamagaluru",
                "State INT Chikkamagaluru", "DCRE Chikkamagaluru", "Lokayukta Chikkamagaluru", "ESCOM Chikkamagaluru"
            )
            "Chitradurga" -> listOf(
                "Abbinahole PS", "Bharamasagara PS", "Challakere PS", "Chikkajajur PS",
                "Chitradurga CEN Crime PS", "Chitradurga Extension PS", "Chitradurga Kote PS",
                "Chitradurga Rural PS", "Chitradurga Town PS", "Chitradurga Traffic PS",
                "Chitradurga Women PS", "Chitrahalli Gate PS", "Hiriyur Rural PS", "Hiriyur Town PS",
                "Holalkere PS", "Hosadurga PS", "Imangala PS", "Molakalmuru PS", "Nayakanhatti PS",
                "Parasurampura PS", "Rampura PS", "Srirampura PS", "Thalak PS", "Turuvanur (Jogi) PS",
                "Control Room Chitradurga", "DPO Chitradurga", "Computer Sec Chitradurga", "DAR Chitradurga",
                "FPB Chitradurga", "MCU Chitradurga", "DCRB Chitradurga", "DSB Chitradurga", "SMMC Chitradurga",
                "State INT Chitradurga", "DCRE Chitradurga", "Lokayukta Chitradurga", "ESCOM Chitradurga"
            )
            "Dakshina Kannada" -> listOf(
                "Bantwala Rural PS", "Bantwala Town PS", "Bantwala Traffic PS", "Bellare PS", "Belthangadi PS",
                "Belthangadi Traffic PS", "Dharmasthala PS", "DK CEN Crime PS", "DK Women PS", "Kadaba PS",
                "Punjalkatte PS", "Puttur Rural PS", "Puttur Town PS", "Puttur Traffic PS", "Subramanya PS",
                "Sullia PS", "Uppinangadi PS", "Venoor PS", "Vitla PS",
                "Control Room Dakshina Kannada", "DPO Dakshina Kannada", "Computer Sec Dakshina Kannada", "DAR Dakshina Kannada",
                "FPB Dakshina Kannada", "MCU Dakshina Kannada", "DCRB Dakshina Kannada", "DSB Dakshina Kannada", "SMMC Dakshina Kannada",
                "State INT Dakshina Kannada", "DCRE Dakshina Kannada", "Lokayukta Dakshina Kannada", "ESCOM Dakshina Kannada"
            )
            "Davanagere" -> listOf(
                "Azad Nagar PS", "Basavanagara PS", "Basavapatna PS", "Bilichodu PS", "Channagiri PS",
                "Davanagere CEN Crime PS", "Davanagere Extention PS", "Davanagere Rural PS",
                "Davanagere South Traffic PS", "Davanagere Women PS", "Davangere North Traffic PS",
                "Gandhi Nagar PS", "Hadadi PS", "Harihara Rural PS", "Harihara Town PS", "Honnali PS",
                "Jagalur PS", "KTJ Nagar PS", "Malebennur PS", "Mayakonda PS", "Nyamathi PS", "RMC Yard PS",
                "Santhebennur PS", "Vidyanagar PS",
                "Control Room Davanagere", "DPO Davanagere", "Computer Sec Davanagere", "DAR Davanagere",
                "FPB Davanagere", "MCU Davanagere", "DCRB Davanagere", "DSB Davanagere", "SMMC Davanagere",
                "State INT Davanagere", "DCRE Davanagere", "Lokayukta Davanagere", "ESCOM Davanagere"
            )
            "Dharwad" -> listOf(
                "Alnavar PS", "Annigeri PS", "Dharwad CEN Crime PS", "Dharwad Rural PS", "Dharwad Women PS",
                "Garag PS", "Gudageri PS", "Hubli Rural PS", "Kalaghatagi PS", "Kundagol PS", "Navalgund PS",
                "Control Room Dharwad", "DPO Dharwad", "Computer Sec Dharwad", "DAR Dharwad",
                "FPB Dharwad", "MCU Dharwad", "DCRB Dharwad", "DSB Dharwad", "SMMC Dharwad",
                "State INT Dharwad", "DCRE Dharwad", "Lokayukta Dharwad", "ESCOM Dharwad"
            )
            "Gadag" -> listOf(
                "Betageri PS", "Betageri Extention PS", "Gadag CEN Crime PS", "Gadag Rural PS",
                "Gadag Town PS", "Gadag Traffic PS", "Gadag Women PS", "Gajendragad PS", "Lakshmeshwar PS",
                "Mulagund PS", "Mundargi PS", "Naregal PS", "Nargund PS", "Ron PS", "Shirahatti PS",
                "Control Room Gadag", "DPO Gadag", "Computer Sec Gadag", "DAR Gadag",
                "FPB Gadag", "MCU Gadag", "DCRB Gadag", "DSB Gadag", "SMMC Gadag",
                "State INT Gadag", "DCRE Gadag", "Lokayukta Gadag", "ESCOM Gadag"
            )
            "Hassan" -> listOf(
                "Alur PS", "Arakalagud PS", "Arasikere Rural PS", "Arasikere Town PS", "Arehalli PS",
                "Banavara PS", "Belur PS", "Channarayapatna Rural PS", "Channarayapatna Town PS",
                "Channarayapatna Traffic PS", "Dudda PS", "Gandasi PS", "Goruru PS", "Halebeedu PS",
                "Hallymysore PS", "Hassan CEN Crime PS", "Hassan City PS", "Hassan Extn. PS",
                "Hassan Rural PS", "Hassan Traffic PS", "Hassan Women PS", "Hirisave PS",
                "Holenarasipura Rural PS", "Holenarasipura Town PS", "Javagal PS", "Konanur PS",
                "Nuggehalli PS", "Pension Mohalla PS", "Sakaleshpura Rural PS", "Sakaleshpura Town PS",
                "Shanthigrama PS", "Shravanabelagola PS", "Yeslur PS",
                "Control Room Hassan", "DPO Hassan", "Computer Sec Hassan", "DAR Hassan",
                "FPB Hassan", "MCU Hassan", "DCRB Hassan", "DSB Hassan", "SMMC Hassan",
                "State INT Hassan", "DCRE Hassan", "Lokayukta Hassan", "ESCOM Hassan"
            )
            "Haveri" -> listOf(
                "Adur PS", "Bankapura PS", "Byadagi PS", "Guttal PS", "Halageri PS", "Hanagal PS",
                "Hansabhavi PS", "Haveri CEN Crime PS", "Haveri Rural PS", "Haveri Town PS",
                "Haveri Traffic PS", "Haveri Women PS", "Hirekerur PS", "Hulagur PS", "Kaginele PS",
                "Kumarapattanam PS", "Ranebennur Rural PS", "Ranebennur Town PS", "Ranebennur Traffic PS",
                "Rattihalli PS", "Savanoor PS", "Shiggaon PS", "Tadas PS",
                "Control Room Haveri", "DPO Haveri", "Computer Sec Haveri", "DAR Haveri",
                "FPB Haveri", "MCU Haveri", "DCRB Haveri", "DSB Haveri", "SMMC Haveri",
                "State INT Haveri", "DCRE Haveri", "Lokayukta Haveri", "ESCOM Haveri"
            )
            "Hubballi Dharwad City" -> listOf(
                "APMC Navanagar", "Ashoknagar PS", "Bendigeri PS", "CEN Crime PS Hubballi Dharwad City", 
                "Dharwad Sub-Urban PS", "Dharwad Town PS", "Dharwad Traffic PS", "E and N Crime PS HD city", 
                "Ghantikeri PS", "Gokul Road PS", "HD City Women PS", "Hubballi East Traffic PS", 
                "Hubballi North Traffic PS", "Hubballi South Traffic PS", "Hubballi Sub Urban PS", 
                "Hubballi Town PS", "Kamaripeth PS", "Kasabapeth PS", "Keshavapur PS", "Old Hubballi PS", 
                "Vidyagiri PS", "Vidyanagar PS",
                "Control Room Hubballi Dharwad City", "DPO Hubballi Dharwad City", "Computer Sec Hubballi Dharwad City", "DAR Hubballi Dharwad City",
                "FPB Hubballi Dharwad City", "MCU Hubballi Dharwad City", "DCRB Hubballi Dharwad City", "DSB Hubballi Dharwad City", "SMMC Hubballi Dharwad City",
                "State INT Hubballi Dharwad City", "DCRE Hubballi Dharwad City", "Lokayukta Hubballi Dharwad City", "ESCOM Hubballi Dharwad City"
            )
            "K.G.F" -> listOf(
                "Andersonpet PS", "Bangarpet PS", "BEML Nagar PS", "Bethamangala PS", "Budikote PS",
                "Champion Reefs PS", "Kamasamudram PS", "KGF CEN Crime PS", "Kyasamballi PS",
                "Marikuppam PS", "Oorgaum PS", "Robertsonpet PS",
                "Control Room K.G.F", "DPO K.G.F", "Computer Sec K.G.F", "DAR K.G.F",
                "FPB K.G.F", "MCU K.G.F", "DCRB K.G.F", "DSB K.G.F", "SMMC K.G.F",
                "State INT K.G.F", "DCRE K.G.F", "Lokayukta K.G.F", "ESCOM K.G.F"
            )
            "Kalaburagi" -> listOf(
                "Afzalpur PS", "Alland PS", "Chincholi PS", "Chittapura PS", "Devalagangapur PS", "Jewargi PS",
                "Kalaburagi CEN Crime PS", "Kalaburagi Women PS", "Kalagi PS", "Kamalapur PS", "Kunchavaram PS",
                "Kurakunta PS", "Madanahipparga PS", "Madbool PS", "Mahagoan PS", "Malkhed PS", "Miryan PS",
                "Mudhol PS", "Narona PS", "Nelogi PS", "Nimbarga PS", "Ratkal PS", "Revoor PS", "Sedam PS",
                "Shahabad Town PS", "Sulepet PS", "Wadi PS", "Yedrami PS",
                "Control Room Kalaburagi", "DPO Kalaburagi", "Computer Sec Kalaburagi", "DAR Kalaburagi",
                "FPB Kalaburagi", "MCU Kalaburagi", "DCRB Kalaburagi", "DSB Kalaburagi", "SMMC Kalaburagi",
                "State INT Kalaburagi", "DCRE Kalaburagi", "Lokayukta Kalaburagi", "ESCOM Kalaburagi"
            )
            "Kalaburagi City" -> listOf(
                "Ashoknagar PS", "Brahmapur PS", "Chowk PS", "Ferhatabad PS", "Kalaburagi City CENCrime PS", 
                "Kalaburagi City Women PS", "Kalaburagi Traffic I PS", "Kalaburagi Traffic II PS", "MB Nagar PS", 
                "Ragavendranagar PS", "Roza PS", "Station Bazar PS", "Sub Urban PS", "University PS",
                "Control Room Kalaburagi City", "DPO Kalaburagi City", "Computer Sec Kalaburagi City", "DAR Kalaburagi City",
                "FPB Kalaburagi City", "MCU Kalaburagi City", "DCRB Kalaburagi City", "DSB Kalaburagi City", "SMMC Kalaburagi City",
                "State INT Kalaburagi City", "DCRE Kalaburagi City", "Lokayukta Kalaburagi City", "ESCOM Kalaburagi City"
            )
            "Kodagu" -> listOf(
                "Bhagamandala PS", "Gonikoppa PS", "Kodagu CEN Crime PS", "Kodagu Women PS",
                "Kushalanagar Rural PS", "Kushalnagar Town PS", "Kushalnagar Traffic PS", "Kutta PS",
                "Madikeri Rural PS", "Madikeri Town PS", "Madikeri Traffic PS", "Napoklu PS",
                "Ponnampet PS", "Shanivarasanthe PS", "Siddapura PS", "Somwarpet PS", "Srimangala PS",
                "Sunticoppa PS", "Virajpet Rural PS", "Virajpet Town PS",
                "Control Room Kodagu", "DPO Kodagu", "Computer Sec Kodagu", "DAR Kodagu",
                "FPB Kodagu", "MCU Kodagu", "DCRB Kodagu", "DSB Kodagu", "SMMC Kodagu",
                "State INT Kodagu", "DCRE Kodagu", "Lokayukta Kodagu", "ESCOM Kodagu"
            )
            "Kolar" -> listOf(
                "Gownapalli PS", "Gulpet PS", "Kolar CEN Crime PS", "Kolar Rural PS", "Kolar Town PS",
                "Kolar Traffic PS", "Kolar Women PS", "Malur PS", "Masti PS", "Mulbagal Rural PS",
                "Mulbagal Town PS", "Nangli PS", "Rayalpad PS", "Srinivasapura PS", "Vemagal PS",
                "Control Room Kolar", "DPO Kolar", "Computer Sec Kolar", "DAR Kolar",
                "FPB Kolar", "MCU Kolar", "DCRB Kolar", "DSB Kolar", "SMMC Kolar",
                "State INT Kolar", "DCRE Kolar", "Lokayukta Kolar", "ESCOM Kolar"
            )
            "Koppal" -> listOf(
                "Alwandi PS", "Bevoor PS", "Gangavathi Rural PS", "Gangavathi Town PS", "Gangavathi Traffic PS",
                "Hanumasagar PS", "Kanakagiri PS", "Karatagi PS", "Koppal CEN Crime PS", "Koppal Rural PS",
                "Koppal Town PS", "Koppal Traffic PS", "Koppal Women PS", "Kuknoor PS", "Kushtagi PS",
                "Munirabad PS", "Tavaregera PS", "Yelburga PS",
                "Control Room Koppal", "DPO Koppal", "Computer Sec Koppal", "DAR Koppal",
                "FPB Koppal", "MCU Koppal", "DCRB Koppal", "DSB Koppal", "SMMC Koppal",
                "State INT Koppal", "DCRE Koppal", "Lokayukta Koppal", "ESCOM Koppal"
            )
            "Mandya" -> listOf(
                "Arakere PS", "Basaralu PS", "Belakavadi PS", "Bellur PS", "Besagaraghalli PS",
                "Bindiganavile PS", "Halagur PS", "K.M. Doddi PS", "K.R. Pet Rural PS", "K.R. Pet Town PS",
                "K.R. Sagar PS", "Keragodu PS", "Kesthur PS", "Kikkeri PS", "Kirugavalu PS", "Koppa PS",
                "Maddur PS", "Maddur Traffic PS", "Malavalli Rural PS", "Malavalli Town PS",
                "Mandya CEN Crime PS", "Mandya Central PS", "Mandya East PS", "Mandya Rural PS",
                "Mandya Traffic PS", "Mandya West PS", "Mandya Women PS", "Melukote PS",
                "Nagamangala Rural PS", "Nagamangala Town PS", "Pandavapura PS", "Shivalli PS",
                "Srirangapatna PS", "Srirangapatna Rural PS",
                "Control Room Mandya", "DPO Mandya", "Computer Sec Mandya", "DAR Mandya",
                "FPB Mandya", "MCU Mandya", "DCRB Mandya", "DSB Mandya", "SMMC Mandya",
                "State INT Mandya", "DCRE Mandya", "Lokayukta Mandya", "ESCOM Mandya"
            )
            "Mangaluru City" -> listOf(
                "Bajpe PS", "Barke PS", "CEN Crime PS Mangaluru City", "E and N Crime PS Mangaluru City", 
                "Kankanady Town PS", "Kavoor PS", "Mangalore East PS", "Mangalore East Traffic PS", 
                "Mangalore North PS", "Mangalore Rural PS", "Mangalore South PS", "Mangalore West Traffic PS", 
                "Mangalore Women PS", "Moodabidre PS", "Mulki PS", "Surathkal PS", "Traffic North Police Station", 
                "Traffic South Police Station", "Ullal PS", "Urva PS",
                "Control Room Mangaluru City", "DPO Mangaluru City", "Computer Sec Mangaluru City", "DAR Mangaluru City",
                "FPB Mangaluru City", "MCU Mangaluru City", "DCRB Mangaluru City", "DSB Mangaluru City", "SMMC Mangaluru City",
                "State INT Mangaluru City", "DCRE Mangaluru City", "Lokayukta Mangaluru City", "ESCOM Mangaluru City"
            )
            "Mysuru City" -> listOf(
                "Alanahally PS", "Ashokpuram PS", "CEN Crime PS Mysuru City", "Devaraja PS", "Deveraja Traffic PS", 
                "E and N Crime PS Mysuru City", "Hebbal PS", "Jayalakshmipuram PS", "Krishnaraja PS", 
                "Krishnaraja Traffic PS", "Kuvempunagar PS", "Lashkar PS", "Laxmipuram PS", "Mandi PS", 
                "Metagalli PS", "Mysuru City Women PS", "Narasimharaja PS", "Narasimharaja Traffic PS", 
                "Nazarbad PS", "Saraswathipuram PS", "Siddarthanagar Traffic PS", "Udayagiri PS", 
                "V V Puram Traffic PS", "V.V. Puram PS", "Vidyaranyarpuram PS", "Vijayanagar PS",
                "Control Room Mysuru City", "DPO Mysuru City", "Computer Sec Mysuru City", "DAR Mysuru City",
                "FPB Mysuru City", "MCU Mysuru City", "DCRB Mysuru City", "DSB Mysuru City", "SMMC Mysuru City",
                "State INT Mysuru City", "DCRE Mysuru City", "Lokayukta Mysuru City", "ESCOM Mysuru City"
            )
            "Mysuru Dist" -> listOf(
                "Bannur PS", "Beechanahalli PS", "Bettadapura PS", "Biligere PS", "Bilikere PS", "Bylakuppe PS",
                "H.D. Kote PS", "Hullahalli PS", "Hunusur Rural PS", "Hunusur Town PS", "Jayapura PS",
                "K.R. Nagar PS", "Kowlande PS", "Mysuru CEN Crime PS", "Mysuru South PS", "Mysuru Women PS",
                "Nanjanagudu Rural PS", "Nanjanagudu Town PS", "Nanjangudu Traffic PS", "Periyapatna PS",
                "Saligrama PS", "Saragur PS", "T. Narasipura PS", "Talakadu PS", "Varuna PS", "Yelawala PS",
                "Control Room Mysuru Dist", "DPO Mysuru Dist", "Computer Sec Mysuru Dist", "DAR Mysuru Dist",
                "FPB Mysuru Dist", "MCU Mysuru Dist", "DCRB Mysuru Dist", "DSB Mysuru Dist", "SMMC Mysuru Dist",
                "State INT Mysuru Dist", "DCRE Mysuru Dist", "Lokayukta Mysuru Dist", "ESCOM Mysuru Dist"
            )
            "Raichur" -> listOf(
                "Balaganoor PS", "Devadurga PS", "Devadurga Traffic PS", "Gabbur PS", "Hutti PS", "Idapanur PS",
                "Jalhalli PS", "Kowthal PS", "Lingasugur PS", "Manvi PS", "Market Yard PS", "Maski PS",
                "Mudgal PS", "Netajinagar PS", "Raichur CEN Crime PS", "Raichur Rural PS", "Raichur Traffic PS",
                "Raichur West PS", "Raichur Women PS", "Sadar Bazar PS", "Shakti Nagar PS", "Sindanoor Rural PS",
                "Sindanur Traffic PS", "Sindhanoor Town PS", "Sirwar PS", "Turvihal PS", "Yapaladinni PS",
                "Yeregera PS",
                "Control Room Raichur", "DPO Raichur", "Computer Sec Raichur", "DAR Raichur",
                "FPB Raichur", "MCU Raichur", "DCRB Raichur", "DSB Raichur", "SMMC Raichur",
                "State INT Raichur", "DCRE Raichur", "Lokayukta Raichur", "ESCOM Raichur"
            )
            "Ramanagara" -> listOf(
                "Akkur PS", "Bidadi PS", "Channapatna East PS", "Channapatna Rural PS", "Channapatna Town PS",
                "Channapatna Traffic PS", "Harohalli PS", "Ijoor PS", "Kaggalipura PS", "Kanakapura Rural PS",
                "Kanakapura Town PS", "Kanakapura Traffic PS", "Kodihalli PS", "Kudur PS", "Kumbalagudu PS",
                "M.K. Doddi PS", "Magadi PS", "Ramanagara CEN Crime PS", "Ramanagara Rural PS",
                "Ramanagara Town PS", "Ramanagara Traffic PS", "Ramanagara Women PS", "Sathanoor PS",
                "Tavarekere PS",
                "Control Room Ramanagara", "DPO Ramanagara", "Computer Sec Ramanagara", "DAR Ramanagara",
                "FPB Ramanagara", "MCU Ramanagara", "DCRB Ramanagara", "DSB Ramanagara", "SMMC Ramanagara",
                "State INT Ramanagara", "DCRE Ramanagara", "Lokayukta Ramanagara", "ESCOM Ramanagara"
            )
            "Shivamogga" -> listOf(
                "Agumbe PS", "Anandapura PS", "Anavatti PS", "Bhadravathi New Town PS",
                "Bhadravathi Old Town PS", "Bhadravathi Rural PS", "Bhadravathi Traffic PS", "Doddapete PS",
                "Holehonnur PS", "Hosamane PS", "Hosanagara PS", "Jayanagara PS", "Kargal PS", "Kote PS",
                "Kumsi PS", "Malur PS", "Nagara PS", "Paper Town PS", "Ripponpet PS", "Sagar Rural PS",
                "Sagar Town PS", "Shikaripura Rural PS", "Shikaripura Town PS", "Shiralakoppa PS",
                "Shivamogga CEN Crime PS", "Shivamogga East Traffic PS", "Shivamogga Rural PS",
                "Shivamogga West Traffic PS", "Shivamogga Women PS", "Soraba PS", "Thirthahalli PS",
                "Tunga Nagar PS", "Vinobanagara PS",
                "Control Room Shivamogga", "DPO Shivamogga", "Computer Sec Shivamogga", "DAR Shivamogga",
                "FPB Shivamogga", "MCU Shivamogga", "DCRB Shivamogga", "DSB Shivamogga", "SMMC Shivamogga",
                "State INT Shivamogga", "DCRE Shivamogga", "Lokayukta Shivamogga", "ESCOM Shivamogga"
            )
            "Tumakuru" -> listOf(
                "Amruthur PS", "Arasikere PS", "Badavanahalli PS", "Bellavi PS", "Chandrashekarpura PS",
                "Chelur PS", "Chikkanayakanahalli PS", "Dandina Shivara PS", "Gubbi PS", "Handanakere PS",
                "Hebbur PS", "Honnavalli PS", "Huliyar PS", "Huliyurdurga PS", "Jayanagara PS",
                "Kallambella PS", "Kibbanhalli PS", "Kodigenahalli PS", "Kolala PS", "Kora PS",
                "Koratagere PS", "Kunigal PS", "Kyathasandra PS", "Madhugiri PS", "Medigeshi PS",
                "New Extention PS", "Nonavinakere PS", "Patanayakanahalli PS", "Pavagada PS", "Sira PS",
                "Tavarekere PS", "Thilak Park PS", "Thirumani PS", "Tiptur Rural PS", "Tiptur Town PS",
                "Tumakuru CEN Crime PS", "Tumakuru Rural PS", "Tumakuru Town PS", "Tumakuru Traffic PS",
                "Tumakuru Women PS", "Turuvekere PS", "Y.N. Hosakote PS",
                "Control Room Tumakuru", "DPO Tumakuru", "Computer Sec Tumakuru", "DAR Tumakuru",
                "FPB Tumakuru", "MCU Tumakuru", "DCRB Tumakuru", "DSB Tumakuru", "SMMC Tumakuru",
                "State INT Tumakuru", "DCRE Tumakuru", "Lokayukta Tumakuru", "ESCOM Tumakuru"
            )
            "Udupi" -> listOf(
                "Ajekar PS", "Amasebailu PS", "Brahmavar PS", "Byndoor PS", "Gangolli PS", "Hebri PS",
                "Hiriadka PS", "Kapu PS", "Karkala Rural PS", "Karkala Town PS", "Kollur PS", "Kota PS",
                "Kundapura PS", "Kundapura Rural PS", "Kundapura Traffic PS", "Malpe PS", "Manipal PS",
                "Padubidri PS", "Shankaranarayana PS", "Shirva PS", "Udupi CEN Crime PS", "Udupi Town PS",
                "Udupi Traffic PS", "Udupi Women PS",
                "Control Room Udupi", "DPO Udupi", "Computer Sec Udupi", "DAR Udupi",
                "FPB Udupi", "MCU Udupi", "DCRB Udupi", "DSB Udupi", "SMMC Udupi",
                "State INT Udupi", "DCRE Udupi", "Lokayukta Udupi", "ESCOM Udupi"
            )
            "Uttara Kannada" -> listOf(
                "Ambikanagar PS", "Ankola PS", "Banavasi PS", "Bhatkal Rural PS", "Bhatkal Town PS",
                "Chittakula PS", "Dandeli Rural PS", "Dandeli Town PS", "Gokarna PS", "Haliyal PS",
                "Honnavara PS", "Joida PS", "Kadra PS", "Karwar Railway PS", "Karwar Rural PS",
                "Karwar Town PS", "Karwar Traffic PS", "Kumta PS", "Mallapura PS", "Manki PS",
                "Mundgod PS", "Murudeshwar PS", "Ramanagar PS", "Siddapura PS", "Sirsi New Market PS",
                "Sirsi Rural PS", "Sirsi Town PS", "UK CEN Crime PS", "UK Women PS", "Yellapura PS",
                "Control Room Uttara Kannada", "DPO Uttara Kannada", "Computer Sec Uttara Kannada", "DAR Uttara Kannada",
                "FPB Uttara Kannada", "MCU Uttara Kannada", "DCRB Uttara Kannada", "DSB Uttara Kannada", "SMMC Uttara Kannada",
                "State INT Uttara Kannada", "DCRE Uttara Kannada", "Lokayukta Uttara Kannada", "ESCOM Uttara Kannada"
            )
            "Vijayanagara" -> listOf(
                "Arasikere PS", "Chigateri PS", "Chittavadagi PS", "Gudekote PS", "Hadagali PS",
                "Hagaribommanahalli PS", "Halavagilu PS", "Hampi Tourism PS", "Hampi Traffic PS",
                "Harapanahalli PS", "Hirehadagali PS", "Hosahalli PS", "Hospet Extention PS",
                "Hospet Rural PS", "Hospet Town PS", "Hospet Traffic PS", "Ittigi PS", "Kamalapur PS",
                "Kottur PS", "Kudligi PS", "Mariyammanahalli PS", "T.B. Dam PS", "T.B. Halli PS",
                "Control Room Vijayanagara", "DPO Vijayanagara", "Computer Sec Vijayanagara", "DAR Vijayanagara",
                "FPB Vijayanagara", "MCU Vijayanagara", "DCRB Vijayanagara", "DSB Vijayanagara", "SMMC Vijayanagara",
                "State INT Vijayanagara", "DCRE Vijayanagara", "Lokayukta Vijayanagara", "ESCOM Vijayanagara"
            )
            "Vijayapur" -> listOf(
                "Adarsh Nagar PS", "Alamatti PS", "Almel PS", "APMC PS", "Babaleshwar PS",
                "Basavan Bagewadi PS", "Chadachan PS", "Devara Hipparagi PS", "Gandhi Chowk PS",
                "Golgumbaz PS", "Hortti PS", "Indi PS", "Indi Rural PS", "Jalanagar PS", "Kalkeri PS",
                "Kolhar PS", "Kudagi PS", "Managuli PS", "Muddebihal PS", "Nidagundi PS", "Sindagi PS",
                "Talikot PS", "Tikota PS", "Vijayapura CEN Crime PS", "Vijayapura Rural PS",
                "Vijayapura Traffic PS", "Vijayapura Women PS", "Zalaki PS",
                "Control Room Vijayapur", "DPO Vijayapur", "Computer Sec Vijayapur", "DAR Vijayapur",
                "FPB Vijayapur", "MCU Vijayapur", "DCRB Vijayapur", "DSB Vijayapur", "SMMC Vijayapur",
                "State INT Vijayapur", "DCRE Vijayapur", "Lokayukta Vijayapur", "ESCOM Vijayapur"
            )
            "Yadgir" -> listOf(
                "Bheemarayanagudi PS", "Gogi PS", "Gurumitkal PS", "Hunasagi PS", "Kembhavi PS",
                "Kodekal PS", "Narayanapura PS", "Saidapur PS", "Shahapur PS", "Shorapur PS",
                "Wadigere PS", "Yadgiri CEN Crime PS", "Yadgiri Rural PS", "Yadgiri Town PS",
                "Yadgiri Traffic PS", "Yadgiri Women PS",
                "Control Room Yadgir", "DPO Yadgir", "Computer Sec Yadgir", "DAR Yadgir",
                "FPB Yadgir", "MCU Yadgir", "DCRB Yadgir", "DSB Yadgir", "SMMC Yadgir",
                "State INT Yadgir", "DCRE Yadgir", "Lokayukta Yadgir", "ESCOM Yadgir"
            )
            else -> emptyList()
        }

        // Add district name itself and sort
        (specificStations + districtName).distinct().sorted()
    }
}

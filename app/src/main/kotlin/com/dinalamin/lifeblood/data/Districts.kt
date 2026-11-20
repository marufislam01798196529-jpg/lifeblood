package com.dinalamin.lifeblood.data

object Districts {

    // Flat list used to populate simple Spinners (division headers omitted;
    // shown as a plain alphabetical-by-division list, same 64 districts as the web app).
    val all: List<String> = listOf(
        // Dhaka Division
        "Dhaka", "Faridpur", "Gazipur", "Gopalganj", "Kishoreganj", "Madaripur",
        "Manikganj", "Munshiganj", "Narayanganj", "Narsingdi", "Rajbari", "Shariatpur", "Tangail",
        // Chattogram Division
        "Bandarban", "Brahmanbaria", "Chandpur", "Chattogram", "Comilla", "Cox's Bazar",
        "Feni", "Khagrachhari", "Lakshmipur", "Noakhali", "Rangamati",
        // Rajshahi Division
        "Bogura", "Chapainawabganj", "Joypurhat", "Naogaon", "Natore", "Pabna", "Rajshahi", "Sirajganj",
        // Khulna Division
        "Bagerhat", "Chuadanga", "Jashore", "Jhenaidah", "Khulna", "Kushtia",
        "Magura", "Meherpur", "Narail", "Satkhira",
        // Barishal Division
        "Barguna", "Barishal", "Bhola", "Jhalokati", "Patuakhali", "Pirojpur",
        // Sylhet Division
        "Habiganj", "Moulvibazar", "Sunamganj", "Sylhet",
        // Rangpur Division
        "Dinajpur", "Gaibandha", "Kurigram", "Lalmonirhat", "Nilphamari", "Panchagarh", "Rangpur", "Thakurgaon",
        // Mymensingh Division
        "Jamalpur", "Mymensingh", "Netrokona", "Sherpur"
    )

    val bloodGroups: List<String> = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
}

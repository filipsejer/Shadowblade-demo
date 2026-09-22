package game;

/** Which look a level has. */
enum Theme {
    FOREST("forest", "The Whispering Forest"),
    CITY("city", "The Neon City"),
    LAB("lab", "The Mad Scientist's Laboratory");

    /** Prefix of this theme's creature sprites in the atlas ("forest.grunt.walk", ...). */
    final String key;
    final String title;

    Theme(String key, String title) {
        this.key = key;
        this.title = title;
    }
}

// Header scroll effect
window.addEventListener('scroll', function () {
    const header = document.querySelector('header');
    if (window.scrollY > 50) {
        header.classList.add('scrolled');
    } else {
        header.classList.remove('scrolled');
    }
});

// Mobile Navigation Menu Toggle
document.addEventListener('DOMContentLoaded', () => {
    const hamburgerBtn = document.querySelector('.hamburger-menu-button');
    const closeMenuBtn = document.querySelector('.close-menu-button');
    const mobileNavMenu = document.querySelector('.mobile-nav-menu');
    const mobileNavOverlay = document.querySelector('.mobile-nav-overlay');
    const mobileNavLinks = document.querySelectorAll('.mobile-nav-links ul li a');

    if (hamburgerBtn && mobileNavMenu && closeMenuBtn && mobileNavOverlay) {
        hamburgerBtn.addEventListener('click', () => {
            mobileNavMenu.classList.add('active');
            mobileNavOverlay.classList.add('active');
        });

        closeMenuBtn.addEventListener('click', () => {
            mobileNavMenu.classList.remove('active');
            mobileNavOverlay.classList.remove('active');
        });

        mobileNavOverlay.addEventListener('click', () => {
            mobileNavMenu.classList.remove('active');
            mobileNavOverlay.classList.remove('active');
        });

        mobileNavLinks.forEach(link => {
            link.addEventListener('click', () => {
                mobileNavMenu.classList.remove('active');
                mobileNavOverlay.classList.remove('active');
            });
        });
    }
});

// Update Data (Centralized for both pages)
const updateData = {
    update1: {
        image: "assets/update.jpg",
        headline: "Rhythm's Website Launch ;)",
        date: "August 27, 2025",
        writer: "Anjishnu Nandi",
        details: `
            <p>We're excited to announce the launch of our new website, designed to provide a better user experience and easier access to all things related to Rhythm.</p>
        `
    },
    update2: {
        image: "assets/Banner.png",
        headline: "2.7 Stable Update is here!",
        date: "August 17, 2025",
        writer: "Anjishnu Nandi",
        details: `
            <p>Greetings, Rhythm users! We're excited to bring you a maintenance update focused on enhancing your overall experience. This release addresses several key areas, bringing new features, crucial fixes, and performance improvements.</p>
            <h3>What's New:</h3>
            <ul>
                <li>Added: Play All & Mix to few Widgets (Home)</li>
                <li>Fixed: Crash due to "IllegalArgumentException: colors must have length of at least 2 if colorStops is omitted" (Player)</li>
                <li>Fixed: Tab Switch sometimes skipped Playlists Tab (Library)</li>
                <li>Improved: Tweaked color schemes for Controls based on their functions (Player)</li>
                <li>Improved: Filters (Library)</li>
                <li>Many more optimizations & improvements...</li>
            </ul>
            <h3>Known Issues (Will be fixed on a later build):</h3>
            <ul>
                <li>Few artist images not showing up</li>
                <li>Canvas needs more optimization</li>
                <li>Shuffle needs more improvement</li>
                <li>Reinstalling Rhythm skips On-Boarding and Media Scanning after launching</li>
            </ul>
            <h3>Build Information:</h3>
            <ul>
                <li>Build: 503</li>
                <li>Type: Stable Release.</li>
            </ul>
            <p>Thank you for your continued support!</p>
        `
    },
    update3: {
        image: "assets/updates2.png",
        headline: "October 2025 Update: Major Improvements In Development!",
        date: "October 5, 2025",
        writer: "Anjishnu Nandi",
        details: `
            <p>Hey there, Rhythm community! We're thrilled to share our ongoing development progress with you. Since our solid 2.7 stable release, we've been pushing boundaries and adding features that will elevate your music experience to new heights.</p>
            <h3>What's Currently In Development:</h3>
            <h4>🎵 New Features & Enhancements:</h4>
            <ul>
                <li><strong>Sleep Timer</strong> - Customizable sleep timers with flexible duration options</li>
                <li><strong>Audio Equalizer</strong> - Professional-grade equalizer for fine-tuning your sound</li>
                <li><strong>Playlist Management</strong> - Advanced playlist import/export capabilities with backup support</li>
                <li><strong>Library Redesign</strong> - Complete library overhaul with single card layout and improved navigation</li>
                <li><strong>Metadata Editing</strong> - Edit song metadata including artwork, with permission handling</li>
                <li><strong>Genre-Based Filtering</strong> - Smart genre detection and filtering throughout the app</li>
                <li><strong>File Explorer</strong> - New Explorer tab with folder-based song grouping and storage access</li>
                <li><strong>Theme Customization</strong> - Custom color schemes, fonts, and theme previews</li>
                <li><strong>Enhanced Navigation</strong> - Improved navigation with customizable library tab order</li>
                <li><strong>Device Output Management</strong> - Better audio device switching and output handling</li>
                <li><strong>Cache & Performance</strong> - Advanced cache management and performance optimizations</li>
            </ul>
            <h4>🔧 Technical Improvements:</h4>
            <ul>
                <li>Enhanced lyrics fetching with better error handling</li>
                <li>Robust genre detection and UI integration</li>
                <li>Album art color extraction for dynamic theming</li>
                <li>Improved search functionality across settings</li>
                <li>Enhanced haptics and accessibility features</li>
                <li>Mobile-responsive navigation and UI improvements</li>
            </ul>
            <h3>🚀 Our Commitment to Progress:</h3>
            <p>We're diligently working on bringing all these features together into a comprehensive, stable release. Our focus remains on maintaining the stability you expect while pushing the boundaries of what's possible with a Material You music player.</p>
            <h3>🎯 Development Priorities:</h3>
            <ul>
                <li>Testing and refining all new features for stability</li>
                <li>Performance optimizations across the entire application</li>
                <li>Bug fixes and user experience improvements based on feedback</li>
                <li>Enhanced stability through rigorous testing</li>
                <li>Adding customization options to make Rhythm truly yours</li>
            </ul>
            <p><strong>Transparent Development:</strong> We believe in keeping our community informed about our progress. While we don't have an exact release date yet, we wanted to share what's currently taking shape. Thank you for your continued support and patience—your feedback helps shape the direction of Rhythm's development!</p>
        `
    },
    update4: {
        image: "assets/Banner.png",
        headline: "2.8 Stable Release is here!",
        date: "October 7, 2025",
        writer: "Anjishnu Nandi",
        details: `
            <p>Exciting news, Rhythm community! Version 2.8 Stable brings a comprehensive update with major new features, performance improvements, and enhanced user experience. This release represents months of dedicated development and incorporates extensive community feedback.</p>
            <h3>🎵 Major New Features:</h3>
            <ul>
                <li><strong>Sleep Timer</strong> - Advanced sleep timer with customizable duration, fade-out options, and playlist continuation</li>
                <li><strong>Professional Audio Equalizer</strong> - 10-band equalizer with presets, custom profiles, and real-time audio processing</li>
                <li><strong>Enhanced Playlist Management</strong> - Import/export playlists, backup/restore functionality, and advanced playlist editing</li>
                <li><strong>Library Redesign</strong> - Complete library overhaul with single card layout, improved navigation, and customizable tab order</li>
                <li><strong>Metadata Editor</strong> - Edit song metadata including artwork, with proper permission handling and batch operations</li>
                <li><strong>Genre-Based Organization</strong> - Smart genre detection, filtering, and organization throughout the library</li>
                <li><strong>File Explorer Integration</strong> - New Explorer tab with folder-based song grouping and advanced storage access</li>
                <li><strong>Theme Customization</strong> - Custom color schemes, font selection, and comprehensive theme previews</li>
                <li><strong>Device Output Management</strong> - Enhanced audio device switching, Bluetooth handling, and output optimization</li>
                <li><strong>Advanced Cache Management</strong> - Intelligent cache system with manual controls and performance monitoring</li>
            </ul>
            <h3>🔧 Performance & Stability Improvements:</h3>
            <ul>
                <li>Enhanced lyrics fetching with improved error handling and caching</li>
                <li>Robust genre detection with better accuracy and UI integration</li>
                <li>Album art color extraction for dynamic theming and Material You integration</li>
                <li>Improved search functionality across all settings and library sections</li>
                <li>Enhanced haptics and accessibility features throughout the app</li>
                <li>Mobile-responsive navigation with improved touch interactions</li>
                <li>Optimized memory usage and reduced battery consumption</li>
                <li>Faster app startup and smoother transitions</li>
            </ul>
            <h3>🐛 Bug Fixes:</h3>
            <ul>
                <li>Fixed: Reinstalling Rhythm now properly resets onboarding and media scanning</li>
                <li>Fixed: Crash due to color gradient issues in player controls</li>
                <li>Fixed: Tab switching inconsistencies in library navigation</li>
                <li>Fixed: Artist images not displaying correctly in some cases</li>
                <li>Fixed: Canvas rendering optimizations for better performance</li>
                <li>Fixed: Shuffle algorithm improvements for better randomization</li>
                <li>Fixed: Various UI inconsistencies and Material You theming issues</li>
                <li>Fixed: Permission handling improvements for media access</li>
            </ul>
            <h3>🎨 UI/UX Enhancements:</h3>
            <ul>
                <li>Refined Material You design implementation with better color schemes</li>
                <li>Improved widget functionality with Play All and Mix options</li>
                <li>Enhanced player controls with better visual hierarchy</li>
                <li>Streamlined library filters and search capabilities</li>
                <li>Better onboarding flow with improved user guidance</li>
                <li>Enhanced settings organization and discoverability</li>
            </ul>
            <h3>🙏 Acknowledgments:</h3>
            <p>We want to extend our heartfelt thanks to our amazing community for their continued support, feedback, and patience throughout the development process. Your input has been invaluable in shaping Rhythm into the music player it is today.</p>
            <p>Special thanks to our beta testers and contributors who helped identify issues and suggest improvements. This release wouldn't be possible without your dedication!</p>
            <h3>🔮 Looking Ahead:</h3>
            <p>While 2.8 Stable represents a significant milestone, we're already planning exciting features for future updates. Stay tuned for more announcements and continue to share your feedback through our GitHub repository.</p>
            <p><strong>Happy listening with Rhythm 2.8! 🎵</strong></p>
        `
    },
    update5: {
        image: "assets/Banner.png",
        headline: "2.9 Stable Release is here!",
        date: "October 11, 2025",
        writer: "Anjishnu Nandi",
        details: `
            <p>Exciting news, Rhythm community! Version 2.9 Stable brings critical fixes to media scanning functionality, new features like lyrics editor and play next functionality, and numerous improvements across the app. This release focuses on stability and user experience enhancements.</p>
            <h3>🐛 Critical Fixes:</h3>
            <ul>
                <li><strong>Fixed: Media Scan Mode Not Respected in Filtering Logic (MediaScan)</strong> - The filtering system now properly respects the selected media scan mode (blacklist vs whitelist)</li>
                <li><strong>Fixed: Media Scan Mode Not Updated When Switching Tabs (MediaScan)</strong> - Switching between blacklist and whitelist tabs now properly updates the active filtering mode</li>
                <li><strong>Fixed: Incorrect Folder Path Extraction from Document Tree Picker (MediaScan)</strong> - Folder selection now correctly constructs full paths that match MediaStore file paths</li>
                <li><strong>Fixed: Blacklisted tracks showing up on playlists (MediaScan)</strong> - Playlists now properly respect media scan filtering</li>
            </ul>
            <h3>🎵 New Features:</h3>
            <ul>
                <li><strong>Added: Lyrics Editor (Player)</strong> - Edit and customize lyrics directly in the player interface</li>
                <li><strong>Added: Play Next (Library)</strong> - Add songs to play next in queue from library view</li>
                <li><strong>Added: Tab Reorder now can be accessed directly (Library)</strong> - Quick access to tab reordering functionality</li>
            </ul>
            <h3>🔧 Improvements:</h3>
            <ul>
                <li><strong>Improved: Song item three-dot menu (Player)</strong> - Enhanced context menu with better organization and functionality</li>
                <li><strong>Improved: Added project website information (About)</strong> - Direct links and information about the Rhythm project website</li>
                <li><strong>Fixed: Cleaned up unused permissions (System)</strong> - Removed unnecessary permissions for better privacy</li>
                <li><strong>Fixed: Lazy loading on Artist tab (Library)</strong> - Improved performance when browsing artists</li>
                <li><strong>Fixed: External Storage contents not showing up on explorer (Library)</strong> - Better support for external storage devices</li>
            </ul>
            <h3>📝 Miscellaneous Changes:</h3>
            <ul>
                <li>Lots of miscellaneous changes and optimizations</li>
                <li>Many more optimizations & improvements across the entire application</li>
                <li>Enhanced stability and performance improvements</li>
                <li>Better error handling and user feedback</li>
            </ul>
            <h3>🙏 Acknowledgments:</h3>
            <p>We want to thank our community for reporting the media scanning issues and helping us identify these critical bugs. Your feedback is invaluable in making Rhythm better!</p>
            <p>Special thanks to users who tested the media scanning functionality and provided detailed bug reports that helped us fix these issues quickly.</p>
            <h3>🔮 What's Next:</h3>
            <p>We're continuing to work on additional features and improvements. Stay tuned for more updates and continue to share your feedback through our GitHub repository and community channels.</p>
            <p><strong>Happy listening with Rhythm 2.9! 🎵</strong></p>
        `
    },
    update7: {
        image: "assets/Banner.png",
        headline: "Rhythm Holiday Update 2025: Features & Improvements from v3.2 to v3.7 🎄🎵",
        date: "December 21, 2025",
        writer: "Anjishnu Nandi",
        details: `
            <p>🎄✨ As the year 2025 wraps up like a perfectly mixed playlist, we're here with a holiday special edition of Rhythm updates! From v3.2 to v3.7, we've been busy cooking up features that'll make your music experience sing. "Ho ho ho! Merry updates!" says Santa Code, as he delivers the goods. Let's dive into this festive changelog extravaganza!</p>
            
            <p>🎵 <strong>v3.2 Maintenance Update:</strong> "Hey there, playlist maestro! Want smart playback options?" Our new Playlist Action Dialog lets you play, queue, or replace with style. The Queue Action Dialog? "Play next, add to queue, or replace everything - your call!" Active song highlighting makes sure you never lose your place, and the playlist detail screen brings smart playback options to the party.</p>
            
            <p>🎨 <strong>v3.3 Maintenance Update:</strong> "Behold, the new External Player!" shouts the UI wizard. We've added carousel styles and the ability to reorder sections - "Rearrange to your heart's content!" Home and time formatting settings let you customize your space, while the new Festive Theme Engine brings holiday cheer. "Crash? What crash?" We fixed that pesky NullPointerException from MediaStore, plus missing callbacks and metadata editor woes. "UI, theme, and performance? Improved beyond belief!"</p>
            
            <p>⚙️ <strong>v3.4 Maintenance Update:</strong> "Player Settings and Artist Settings, at your service!" declares the settings guru. Playlists now maintain original order with proper progress tracking. "No more UI, theme, or performance hiccups - we've got you covered!"</p>
            
            <p>🎛️ <strong>v3.5 Stable - Feature Update:</strong> "Gesture control and progress customization? Yes please!" Control gestures on mini and full players, customize progress bars, and check out the new Rhythm Stats screen expanding your widget. "Many mixed UI improvements - smoother than a jazz solo!" Song names now auto-scroll instead of overflowing, and under-the-hood optimizations make everything faster. "Performance boost? Activated!"</p>
            
            <p>🔊 <strong>v3.5 Stable - Maintenance Update:</strong> "AutoEQ for 61 devices? Tuned to perfection!" The equalizer now supports 10 bands with better UI, Rhythm Stats got a glow-up, and more UI/performance improvements. "Shuffle toggle fixed - queue list updates like magic!" Stats widget now follows service data flawlessly. "No more widget woes!"</p>
            
            <p>🎤 <strong>v3.6 Stable - Feature Update:</strong> "AutoEQ presets for 6032 devices? With import/export? Mind blown!" The lyrics parser now combines untimestamped lines with timestamped ones, the Christmas theme got even more festive, and UI/performance improvements abound. "Lyrics and themes - holiday ready!"</p>
            
            <p>📱 <strong>v3.7 Stable - Maintenance Update:</strong> "Playlist screen reordering, sorting, and multi-remove? Check!" Turn default playlists on/off in settings, stop playback when the app's removed from RAM, and enjoy Auto-EQ device preset detection. "Equalizer fixed and UI improved - crystal clear!" Onboarding overflow fixed, library tab reordering issues resolved, player seek and playback enhanced, and translations polished. "Settings, library, player - all upgraded!"</p>
            
            <p>🎄 "Whew, what a ride!" exclaims the development team, wiping sweat from their brows. "From playlist heroes to equalizer wizards, we've packed in features that'll make your holidays merry and bright!"</p>
            
            <p>🎉 As we bid adieu to 2025 and welcome 2026, thank you for being the beat in our Rhythm! May your playlists be filled with joy, your audio crystal clear, and your music experiences unforgettable. Happy New Year, music lovers! 🥂</p>
            
            <p>Thank you for using Rhythm! Report issues on GitHub.</p>
        `
    },
    update8: {
        image: "assets/Banner.png",
        headline: "4.0 Stable Release is here: New Year Groove 2026! 🎊🎵",
        date: "January 7, 2026",
        writer: "Anjishnu Nandi",
        details: `
            <p>🎊 Happy New Year 2026, Rhythm heroes! As fireworks explode and resolutions are made, we're dropping this stable release to kick off the year with a bang! "Rate your songs like a critic!" shouts the Rating System superhero. View your stats in Rhythm Stats - because who doesn't love data?</p>
            
            <p>🔍 "Settings search? Faster than a speeding bullet!" The Search in Settings feature lets you find options quicker than you can say "configuration."</p>
            
            <p>📱 Onboarding Revamp: "Tablet support? Check! Improved setup? Double check!" Welcome new users with style.</p>
            
            <p>🔗 API Integrations: "Refactored for better integrations - smoother than silk!"</p>
            
            <p>🎛️ AutoEQ Enhancements: "Better detection, better fixes - your ears will thank you!"</p>
            
            <p>✨ UI Polish and tablet adaptations - minor but mighty!</p>
            
            <p>🔒 Permission Checks enhanced for Android 9-16 - security first!</p>
            
            <p>🛡️ Stability: Critical crash fixes and memory leak resolutions - no more leaks in your ship!</p>
            
            <p>🐛 Bug Fixes: Carousel crashes? Squashed! LazyColumn issues? Fixed! Spatializer compatibility? Improved! UI glitches? Gone!</p>
            
            <p>🎉 Here's to a year of amazing music, bug-free grooves, and endless playlists! Happy New Year from the Rhythm team! 🥂🎵</p>
            
            <p>Thank you for using Rhythm! Report issues on GitHub.</p>
        `
    },
    update6: {
        image: "assets/Banner.png",
        headline: "3.1 Stable Release is here!",
        date: "December 1, 2025",
        writer: "Anjishnu Nandi",
        details: `
            <p>🎉 Hold onto your headphones, music lovers! Rhythm 3.1 Stable has dropped, and it's PACKED with goodies! We've been cooking up a storm, and this release is our biggest, boldest update yet. From speaking your language (literally!) to widgets that'll make your home screen jealous, we've got it all!</p>
            
            <h3>🌍 Now Speaking YOUR Language!</h3>
            <p>We've gone global, baby! Rhythm now supports multiple languages with a built-in language switcher. Huge shoutout to our amazing community translators who brought French, Dutch, and more to life. Whether you're in Paris, Amsterdam, or anywhere in between, Rhythm speaks YOUR language! 🗣️</p>
            
            <h3>⚙️ Settings Got a MAJOR Glow-Up!</h3>
            <p>Say goodbye to the old boring settings and hello to the sleek new "Tuner" UI! We're talking:</p>
            <ul>
                <li>🎴 <strong>Gorgeous Grouped Cards</strong> - Everything's organized beautifully now</li>
                <li>📖 <strong>Collapsible Headers</strong> - Expand what you need, hide what you don't</li>
                <li>💡 <strong>Quick Tips</strong> - Helpful hints right when you need them</li>
                <li>🔍 <strong>Settings Search</strong> - Find ANY setting in seconds!</li>
                <li>🎨 <strong>Next-Level Theme Customization</strong> - Make Rhythm TRULY yours</li>
            </ul>
            
            <h3>📱 Widget Mania: 9 Flavors of Awesome!</h3>
            <p>Widgets just got a SERIOUS upgrade! Choose from NINE different layouts (from tiny 2x1 to massive 5x5) and customize them to your heart's content. Built with modern Glance framework and Material 3 design, these widgets are eye candy with real-time updates. Your home screen will thank you! 🎉</p>
            
            <h3>🎵 Music Features That Hit Different</h3>
            <p>We've turbocharged your music experience:</p>
            <ul>
                <li>🎯 <strong>Queue Action Dialog</strong> - Play next? Add to queue? Replace everything? YOU decide!</li>
                <li>📊 <strong>Playlist Grid View</strong> - Because sometimes lists are boring</li>
                <li>🎸 <strong>Genre Browsing</strong> - Rock, Jazz, Pop - find it all with smart detection</li>
                <li>✅ <strong>Multi-Select Madness</strong> - Add tons of songs to playlists in one go</li>
                <li>🎛️ <strong>Player Customization</strong> - Arrange those player chips YOUR way</li>
                <li>🏠 <strong>Default Screen Selection</strong> - Start wherever you want</li>
                <li>👁️ <strong>Show/Hide Tabs</strong> - Declutter your library view</li>
            </ul>
            
            <h3>🎤 Lyrics Just Got LYRICAL!</h3>
            <p>Karaoke lovers, REJOICE! We've added:</p>
            <ul>
                <li>✨ <strong>Word-by-Word Highlighting</strong> - Follow along like a pro</li>
                <li>⏱️ <strong>Sync Adjustment</strong> - Lyrics off-beat? Fix it yourself with offset controls</li>
                <li>🎼 <strong>OGG/Vorbis Support</strong> - Extract lyrics from even MORE file types</li>
                <li>🎯 <strong>Better Extraction</strong> - Smarter embedded lyrics detection</li>
            </ul>
            
            <h3>✨ UI That Makes You Go "WOW!"</h3>
            <p>Every corner of Rhythm got prettier:</p>
            <ul>
                <li>🎚️ <strong>Equalizer Makeover</strong> - Slick new cards with buttery animations</li>
                <li>📄 <strong>Bottom Sheet Magic</strong> - Smooth as butter, looks like a dream</li>
                <li>👆 <strong>Swipe to Skip</strong> - Swipe left/right on mini player to change tracks</li>
                <li>🌊 <strong>Fluid Transitions</strong> - Navigation so smooth you'll want to keep tapping</li>
            </ul>
            
            <h3>⚡ Faster Than Your Morning Coffee!</h3>
            <p>Performance nerds (we see you 🤓), check this out:</p>
            <ul>
                <li>🚀 Optimized directory loading - blazing fast even with 10,000+ songs</li>
                <li>⚡ Genre caching - instant genre detection</li>
                <li>🔋 Better battery life - jam longer, charge less</li>
                <li>💾 Smarter memory usage - no more sluggishness</li>
                <li>📂 Enhanced media scanning - finds your music faster</li>
            </ul>
            
            <h3>🐛 Squashed Bugs (RIP Little Guys)</h3>
            <ul>
                <li>✅ Fixed Canvas API hiccups</li>
                <li>✅ No more LazyColumn crashes (they were getting lazy indeed!)</li>
                <li>✅ Queue now finds the RIGHT duplicate song (not just any duplicate)</li>
                <li>✅ "Most Played" respects your scan settings now</li>
                <li>✅ Screen transitions won't crash anymore</li>
                <li>✅ Album Artist grouping works perfectly</li>
                <li>✅ Updater animations are buttery smooth</li>
            </ul>
            
            <h3>🙏 HUGE Thanks to YOU!</h3>
            <p>This wouldn't exist without our INCREDIBLE community! From bug reports to feature suggestions to translations - you all ROCK! Special love to our beta testers who caught issues before they became problems, and our translators who brought Rhythm to the world. You're all legends! 🌟</p>
            
            <h3>🔮 What's Cooking for 3.2?</h3>
            <p>We're already planning the next wave of awesome:</p>
            <ul>
                <li>📝 Persistent metadata editing (save those changes forever!)</li>
                <li>🌍 Even MORE languages from our amazing community</li>
                <li>🎨 Additional widget layouts and customizations</li>
                <li>🎵 Enhanced playlist management</li>
                <li>⚡ More performance magic</li>
            </ul>
            
            <p><strong>⚠️ Quick heads up: Metadata editor is memory-only for now, translations are still coming in, and you might see rare ANRs when idle. We're on it!</strong></p>
            
            <p><strong>🎉 Thank you for being part of this incredible journey! Grab 3.1, crank up the volume, and let's make some noise! Happy listening, Rhythm fam! 🎵✨</strong></p>
        `
    },
    update10: {
        image: "assets/Banner.png",
        headline: "4.2 Stable is Here: The Expressive Update! 🎨🎵",
        date: "March 1, 2026",
        writer: "Anjishnu Nandi",
        details: `
            <p>🎉 <strong>Rhythm 4.2 Stable is live</strong> — and it's our most polished release yet! This update focuses on expressive design, audiophile-grade audio, library power features, and rock-solid stability. Let's dive in!</p>

            <h3>🎨 Expressive UI Refresh</h3>
            <p>Material 3 Expressive is here. Rhythm 4.2 brings redesigned shapes, adaptive components, and refined motion throughout the entire app — from the player to library cards to bottom sheets. Every screen feels more vibrant and alive.</p>

            <h3>🔊 Audiophile-Grade Playback</h3>
            <ul>
                <li><strong>High-Resolution Audio Mode</strong> — Preserve native sample rates and bit depths, bypassing Android processing for purer sound</li>
                <li><strong>EAC3-JOC Decoder</strong> — Full support for Dolby Atmos / EAC3-JOC encoded files</li>
                <li><strong>Extended Codec Coverage</strong> — Improved ALAC, OGG, and more with the updated FFmpeg decoder integration</li>
            </ul>

            <h3>📚 Library Power Features</h3>
            <ul>
                <li><strong>Multi-Select Actions</strong> — Select multiple songs, albums, or playlists and perform batch operations: add to queue, move, delete, share, and more</li>
                <li><strong>Shuffle Whole Albums</strong> — The album shuffle button now queues complete albums in sequence, randomly ordered — perfect for album enthusiasts who love discovering new listens</li>
                <li><strong>Playlist Sort Persistence</strong> — Your playlist sort preferences are now remembered between sessions</li>
                <li><strong>Scroll Position Memory</strong> — The Songs tab restores your scroll position after navigating away and coming back</li>
            </ul>

            <h3>🐛 Fixes & Stability</h3>
            <ul>
                <li><strong>Queue Reliability</strong> — Fixed queue operations that were breaking across multiple screens</li>
                <li><strong>Metadata Editor</strong> — Changes now properly propagate and persist across all screens</li>
                <li><strong>Splash Screen Loop</strong> — Fixed the unnecessary splash screen restart when returning to the app while music was playing</li>
                <li><strong>Media Scanner</strong> — Switched from incremental to full scans to fix the long-standing issue where newly added songs weren't showing up after library modifications</li>
                <li><strong>Faster Load Times</strong> — System startup is noticeably faster thanks to initialization optimizations</li>
                <li><strong>Many more crash fixes, UI polish, and performance improvements throughout the app</strong></li>
            </ul>

            <h3>📦 Build Info</h3>
            <ul>
                <li><strong>Version:</strong> 4.2.334.905</li>
                <li><strong>Type:</strong> Stable Release</li>
                <li><strong>Min Android:</strong> 8.0 (API 26)</li>
            </ul>

            <p>📥 <strong>Download now from <a href="https://github.com/cromaguy/Rhythm/releases/latest" target="_blank">GitHub Releases</a>, F-Droid, or IzzyOnDroid.</strong></p>
            <p>Thanks for being part of Rhythm — your feedback shapes every release. Keep the music playing! 🎵</p>
        `
    },
    update9: {
        image: "assets/Banner.png",
        headline: "Rhythm is Coming to Google Play Store! 📱🎉",
        date: "January 22, 2026",
        writer: "Anjishnu Nandi",
        details: `
            <p>🎊 Exciting news, Rhythm community! We're thrilled to announce that Rhythm will soon be available on the Google Play Store! This is a major milestone in our journey to bring professional music playback to Android users worldwide.</p>
            
            <h3>🚀 What's Coming to Google Play:</h3>
            <ul>
                <li>📱 <strong>Easy Installation</strong> - One-tap install from the Play Store</li>
                <li>🔄 <strong>Automatic Updates</strong> - Seamless updates through Google Play</li>
                <li>🌍 <strong>Global Availability</strong> - Available in all countries where Google Play operates</li>
                <li>🛡️ <strong>Enhanced Security</strong> - Additional security layers through Play Store verification</li>
                <li>📊 <strong>Usage Insights</strong> - Better analytics to improve the app (privacy-focused, of course!)</li>
            </ul>
            
            <h3>🔒 Privacy & Security First:</h3>
            <p>Your privacy remains our top priority. Rhythm has always been designed with privacy in mind, and our Google Play version maintains the same commitment:</p>
            <ul>
                <li>🚫 <strong>No Data Collection</strong> - We don't collect or share personal information</li>
                <li>🔐 <strong>Local Music Only</strong> - All your music stays on your device</li>
                <li>📋 <strong>Minimal Permissions</strong> - Only necessary permissions for music playback</li>
                <li>🛡️ <strong>Open Source</strong> - Full transparency with our GPL v3 license</li>
            </ul>
            
            <h3>⏰ Timeline:</h3>
            <p>We're currently in the final stages of Play Store preparation, including:</p>
            <ul>
                <li>✅ App review and compliance checks</li>
                <li>✅ Store listing optimization</li>
                <li>✅ Beta testing through Play Store</li>
                <li>🔄 Final approvals and publishing</li>
            </ul>
            
            <p><strong>Stay tuned for the official launch announcement!</strong> We'll keep you updated on our progress through our website and GitHub repository.</p>
            
            <h3>📥 Current Download Options:</h3>
            <p>While we prepare for Google Play, you can still download Rhythm from our GitHub releases for the latest features and updates. Our GitHub version will continue to receive updates alongside the Play Store version.</p>
            
            <p><strong>Thank you for your patience and continued support! 🎵✨</strong></p>
        `
    },
};

// Function to check if an image exists
function imageExists(url, callback) {
    const img = new Image();
    img.onload = function() { callback(true); };
    img.onerror = function() { callback(false); };
    img.src = url;
}

// News Carousel Functionality (for index.html)
function setupNewsCarousel() {
    const newsCarouselTrack = document.getElementById('dynamic-news-updates');
    const newsCarouselContainer = document.querySelector('.news-carousel');
    const newsPrevBtn = document.querySelector('.news-prev-btn');
    const newsNextBtn = document.querySelector('.news-next-btn');

    if (!newsCarouselTrack || !newsCarouselContainer || !newsPrevBtn || !newsNextBtn) {
        return; // Exit if news carousel elements are not found
    }

    // Clear existing content
    newsCarouselTrack.innerHTML = '';

    // Populate news items dynamically - sort by date (newest first)
    const sortedUpdateKeys = Object.keys(updateData).sort((a, b) => {
        const dateA = new Date(updateData[a].date);
        const dateB = new Date(updateData[b].date);
        return dateB - dateA; // Newest first
    });

    sortedUpdateKeys.forEach(key => {
        const data = updateData[key];
        const newsItem = document.createElement('div');
        newsItem.className = 'news-carousel-item';
        newsItem.setAttribute('data-update-id', key);

        let imageHtml = `<img src="${data.image}" alt="Update Image">`;
        if (!data.image) {
            imageHtml = `<img src="../assets/icon.png" alt="Rhythm Logo" class="fallback-logo">`;
            newsItem.classList.add('no-image');
        }

        newsItem.innerHTML = `
            ${imageHtml}
            <div class="news-overlay">
                <h3>${data.headline}</h3>
                <p>${data.details.substring(0, 100)}...</p> <!-- Show a summary -->
                <a href="updates.html#${key}" class="btn btn-primary">Read More</a>
            </div>
        `;
        newsCarouselTrack.appendChild(newsItem);
    });

    const newsSlides = document.querySelectorAll('.news-carousel-item');
    let newsSlideIndex = 0;

    function getNewsSlidesPerView() {
        if (window.innerWidth >= 992) {
            return 3;
        } else if (window.innerWidth >= 768) {
            return 2;
        } else {
            return 1;
        }
    }

    function updateNewsCarousel() {
        const slidesPerView = getNewsSlidesPerView();
        const totalSlides = newsSlides.length;
        const slideWidth = newsCarouselContainer.offsetWidth / slidesPerView;

        newsSlides.forEach(slide => {
            slide.style.flex = `0 0 ${100 / slidesPerView}%`;
        });

        if (newsSlideIndex > totalSlides - slidesPerView && totalSlides >= slidesPerView) {
            newsSlideIndex = totalSlides - slidesPerView;
        } else if (newsSlideIndex < 0) {
            newsSlideIndex = 0;
        } else if (newsSlideIndex >= totalSlides) {
            newsSlideIndex = totalSlides - slidesPerView;
        }

        newsCarouselTrack.style.transform = `translateX(-${newsSlideIndex * slideWidth}px)`;
    }

    function showNewsSlide(index) {
        const slidesPerView = getNewsSlidesPerView();
        const totalSlides = newsSlides.length;

        newsSlideIndex = index;

        if (newsSlideIndex < 0) {
            newsSlideIndex = totalSlides - slidesPerView;
        } else if (newsSlideIndex > totalSlides - slidesPerView) {
            newsSlideIndex = 0;
        }
        updateNewsCarousel();
    }

    newsPrevBtn.addEventListener('click', () => {
        showNewsSlide(newsSlideIndex - 1);
    });

    newsNextBtn.addEventListener('click', () => {
        showNewsSlide(newsSlideIndex + 1);
    });

    window.addEventListener('resize', updateNewsCarousel);
    updateNewsCarousel(); // Initial update
    showNewsSlide(0); // Initialize to the first slide

    // Auto-scroll functionality for news carousel
    let newsAutoScrollInterval;
    function startNewsAutoScroll() {
        newsAutoScrollInterval = setInterval(() => {
            showNewsSlide(newsSlideIndex + 1);
        }, 5000); // Change slide every 5 seconds
    }

    function stopNewsAutoScroll() {
        clearInterval(newsAutoScrollInterval);
    }

    // Pause auto-scroll on hover
    newsCarouselContainer.addEventListener('mouseenter', stopNewsAutoScroll);
    newsCarouselContainer.addEventListener('mouseleave', startNewsAutoScroll);

    // Restart auto-scroll when manually navigating
    newsPrevBtn.addEventListener('click', () => {
        stopNewsAutoScroll();
        startNewsAutoScroll();
    });
    newsNextBtn.addEventListener('click', () => {
        stopNewsAutoScroll();
        startNewsAutoScroll();
    });

    startNewsAutoScroll(); // Start auto-scrolling on load
}

// Showcase Carousel Functionality (for index.html)
function setupShowcaseCarousel() {
    const showcaseCarouselTrack = document.getElementById('showcase-carousel-track');
    const showcaseCarouselContainer = document.querySelector('.showcase-carousel');
    const showcasePrevBtn = document.querySelector('.showcase-prev-btn');
    const showcaseNextBtn = document.querySelector('.showcase-next-btn');

    if (!showcaseCarouselTrack || !showcaseCarouselContainer || !showcasePrevBtn || !showcaseNextBtn) {
        return; // Exit if showcase carousel elements are not found
    }

    const showcaseSlides = document.querySelectorAll('.showcase-carousel-item');
    let showcaseSlideIndex = 0;

    function getShowcaseSlidesPerView() {
        if (window.innerWidth >= 992) {
            return 3;
        } else if (window.innerWidth >= 768) {
            return 2;
        } else {
            return 1;
        }
    }

    function updateShowcaseCarousel() {
        const slidesPerView = getShowcaseSlidesPerView();
        const totalSlides = showcaseSlides.length;
        const slideWidth = showcaseCarouselContainer.offsetWidth / slidesPerView;

        showcaseSlides.forEach(slide => {
            slide.style.flex = `0 0 ${100 / slidesPerView}%`;
        });

        if (showcaseSlideIndex > totalSlides - slidesPerView && totalSlides >= slidesPerView) {
            showcaseSlideIndex = totalSlides - slidesPerView;
        } else if (showcaseSlideIndex < 0) {
            showcaseSlideIndex = 0;
        } else if (showcaseSlideIndex >= totalSlides) {
            showcaseSlideIndex = totalSlides - slidesPerView;
        }

        showcaseCarouselTrack.style.transform = `translateX(-${showcaseSlideIndex * slideWidth}px)`;
    }

    function showShowcaseSlide(index) {
        const slidesPerView = getShowcaseSlidesPerView();
        const totalSlides = showcaseSlides.length;

        showcaseSlideIndex = index;

        if (showcaseSlideIndex < 0) {
            showcaseSlideIndex = totalSlides - slidesPerView;
        } else if (showcaseSlideIndex > totalSlides - slidesPerView) {
            showcaseSlideIndex = 0;
        }
        updateShowcaseCarousel();
    }

    showcasePrevBtn.addEventListener('click', () => {
        showShowcaseSlide(showcaseSlideIndex - 1);
    });

    showcaseNextBtn.addEventListener('click', () => {
        showShowcaseSlide(showcaseSlideIndex + 1);
    });

    window.addEventListener('resize', updateShowcaseCarousel);
    updateShowcaseCarousel(); // Initial update
    showShowcaseSlide(0); // Initialize to the first slide

    // Auto-scroll functionality for showcase carousel
    let showcaseAutoScrollInterval;
    function startShowcaseAutoScroll() {
        showcaseAutoScrollInterval = setInterval(() => {
            showShowcaseSlide(showcaseSlideIndex + 1);
        }, 5000); // Change slide every 5 seconds
    }

    function stopShowcaseAutoScroll() {
        clearInterval(showcaseAutoScrollInterval);
    }

    // Pause auto-scroll on hover
    showcaseCarouselContainer.addEventListener('mouseenter', stopShowcaseAutoScroll);
    showcaseCarouselContainer.addEventListener('mouseleave', startShowcaseAutoScroll);

    // Restart auto-scroll when manually navigating
    showcasePrevBtn.addEventListener('click', () => {
        stopShowcaseAutoScroll();
        startShowcaseAutoScroll();
    });
    showcaseNextBtn.addEventListener('click', () => {
        stopShowcaseAutoScroll();
        startShowcaseAutoScroll();
    });

    startShowcaseAutoScroll(); // Start auto-scrolling on load
}

// Screenshot Popup Functionality (for index.html)
function setupScreenshotPopup() {
    const screenshotPopup = document.getElementById('screenshot-popup');
    const screenshotPopupImage = document.getElementById('screenshot-popup-image');
    const screenshotTitle = document.getElementById('screenshot-title');
    const screenshotCounter = document.getElementById('screenshot-counter');
    const closeScreenshotPopupBtn = document.querySelector('.close-screenshot-popup-btn');
    const zoomInBtn = document.getElementById('zoom-in-btn');
    const zoomOutBtn = document.getElementById('zoom-out-btn');
    const zoomResetBtn = document.getElementById('zoom-reset-btn');
    const zoomLevelDisplay = document.getElementById('zoom-level');
    const screenshotPrevBtn = document.getElementById('screenshot-prev-btn');
    const screenshotNextBtn = document.getElementById('screenshot-next-btn');

    if (!screenshotPopup || !screenshotPopupImage || !closeScreenshotPopupBtn) {
        return; // Exit if screenshot popup elements are not found
    }

    // Get all showcase carousel items
    const showcaseSlides = document.querySelectorAll('.showcase-carousel-item img');
    let currentScreenshotIndex = 0;
    let currentZoom = 1;
    const minZoom = 0.5;
    const maxZoom = 3.0;
    const zoomStep = 0.25;
    let isPanning = false;
    let startX, startY, initialX, initialY;

    // Function to get screenshot title from alt text or filename
    function getScreenshotTitle(imgElement) {
        const altText = imgElement.alt;
        const src = imgElement.src;
        const filename = src.split('/').pop().replace('.png', '').replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());

        // Use alt text if it exists and is not generic, otherwise format filename
        if (altText && altText !== 'Screenshot' && altText.includes('Screen')) {
            return altText;
        } else {
            return filename;
        }
    }

    // Function to update zoom level display
    function updateZoomDisplay() {
        const percentage = Math.round(currentZoom * 100);
        zoomLevelDisplay.textContent = `${percentage}%`;
    }

    // Function to apply zoom to image
    function applyZoom(zoomValue) {
        screenshotPopupImage.style.transform = `scale(${zoomValue})`;
        updateZoomDisplay();
    }

    // Function to zoom in
    function zoomIn() {
        if (currentZoom < maxZoom) {
            currentZoom = Math.min(currentZoom * 1.5, maxZoom);
            applyZoom(currentZoom);
        }
    }

    // Function to zoom out
    function zoomOut() {
        if (currentZoom > minZoom) {
            currentZoom = Math.max(currentZoom * 0.75, minZoom);
            applyZoom(currentZoom);
        }
    }

    // Function to reset zoom and position
    function resetZoom() {
        currentZoom = 1;
        screenshotPopupImage.style.transform = 'scale(1) translate(0, 0)';
        updateZoomDisplay();
    }

    // Function to show screenshot popup
    function showScreenshotPopup(index) {
        if (index >= 0 && index < showcaseSlides.length) {
            currentScreenshotIndex = index;
            const imgElement = showcaseSlides[index];
            const imgSrc = imgElement.src;
            const imgAlt = imgElement.alt;
            const title = getScreenshotTitle(imgElement);

            // Update popup content
            screenshotPopupImage.src = imgSrc;
            screenshotPopupImage.alt = imgAlt;
            screenshotTitle.textContent = title;
            screenshotCounter.textContent = `${index + 1} of ${showcaseSlides.length}`;

            // Reset zoom and enable/disable navigation buttons
            resetZoom();
            screenshotPrevBtn.disabled = currentScreenshotIndex === 0;
            screenshotNextBtn.disabled = currentScreenshotIndex === showcaseSlides.length - 1;

            // Show popup
            screenshotPopup.classList.add('active');

            // Pause showcase carousel auto-scroll
            if (typeof window.showcaseAutoScrollInterval !== 'undefined') {
                clearInterval(window.showcaseAutoScrollInterval);
            }
        }
    }

    // Function to hide screenshot popup
    function hideScreenshotPopup() {
        screenshotPopup.classList.remove('active');

        // Resume showcase carousel auto-scroll
        if (showcaseSlides.length > 0) {
            const showcaseCarouselContainer = document.querySelector('.showcase-carousel');
            const showcasePrevBtn = document.querySelector('.showcase-prev-btn');
            const showcaseNextBtn = document.querySelector('.showcase-next-btn');

            window.showcaseAutoScrollInterval = setInterval(() => {
                showcaseNextBtn.click();
            }, 5000); // Restart auto-scroll
        }
    }

    // Add click event listeners to showcase images
    showcaseSlides.forEach((slide, index) => {
        slide.addEventListener('click', () => {
            showScreenshotPopup(index);
        });

        // Make cursor pointer to indicate clickable
        slide.style.cursor = 'pointer';
    });

    // Close popup event listeners
    closeScreenshotPopupBtn.addEventListener('click', hideScreenshotPopup);
    screenshotPopup.addEventListener('click', (e) => {
        if (e.target === screenshotPopup) {
            hideScreenshotPopup();
        }
    });

    // Zoom controls
    zoomInBtn.addEventListener('click', zoomIn);
    zoomOutBtn.addEventListener('click', zoomOut);
    zoomResetBtn.addEventListener('click', resetZoom);

    // Mouse wheel zoom
    screenshotPopupImage.addEventListener('wheel', (e) => {
        e.preventDefault();
        if (e.deltaY < 0) {
            zoomIn();
        } else {
            zoomOut();
        }
    });

    // Pan functionality (drag to move zoomed image)
    screenshotPopupImage.addEventListener('mousedown', (e) => {
        if (currentZoom > 1) {
            isPanning = true;
            startX = e.clientX;
            startY = e.clientY;
            const transform = getComputedStyle(screenshotPopupImage).transform;
            const matrix = new DOMMatrix(transform);
            initialX = matrix.m41 || 0;
            initialY = matrix.m42 || 0;
            screenshotPopupImage.style.cursor = 'grabbing';
        }
    });

    document.addEventListener('mousemove', (e) => {
        if (isPanning && currentZoom > 1) {
            const dx = e.clientX - startX;
            const dy = e.clientY - startY;
            const newX = initialX + dx;
            const newY = initialY + dy;
            screenshotPopupImage.style.transform = `scale(${currentZoom}) translate(${newX}px, ${newY}px)`;
        }
    });

    document.addEventListener('mouseup', () => {
        isPanning = false;
        screenshotPopupImage.style.cursor = currentZoom > 1 ? 'grab' : 'default';
    });

    // Double-click to reset zoom
    screenshotPopupImage.addEventListener('dblclick', resetZoom);

    // Navigation buttons event listeners
    screenshotPrevBtn.addEventListener('click', () => {
        if (currentScreenshotIndex > 0) {
            showScreenshotPopup(currentScreenshotIndex - 1);
        }
    });

    screenshotNextBtn.addEventListener('click', () => {
        if (currentScreenshotIndex < showcaseSlides.length - 1) {
            showScreenshotPopup(currentScreenshotIndex + 1);
        }
    });

    // Keyboard navigation
    document.addEventListener('keydown', (e) => {
        if (!screenshotPopup.classList.contains('active')) return;

        switch(e.key) {
            case 'ArrowLeft':
                if (e.ctrlKey || e.metaKey) {
                    zoomOut();
                } else {
                    screenshotPrevBtn.click();
                }
                break;
            case 'ArrowRight':
                if (e.ctrlKey || e.metaKey) {
                    zoomIn();
                } else {
                    screenshotNextBtn.click();
                }
                break;
            case 'ArrowUp':
                zoomIn();
                break;
            case 'ArrowDown':
                zoomOut();
                break;
            case '0':
            case 'Home':
                resetZoom();
                break;
            case 'Escape':
                hideScreenshotPopup();
                break;
        }
    });

    window.showScreenshotPopup = showScreenshotPopup; // Make it globally accessible
}

// Update Popup Functionality (for updates.html)
function setupUpdatePopup() {
    const updateItems = document.querySelectorAll('.updates-list .update-item');
    const updatePopup = document.getElementById('update-popup');
    const closePopupBtn = document.querySelector('.close-popup-btn');
    const popupImage = document.getElementById('popup-image');
    const popupHeadline = document.getElementById('popup-headline');
    const popupDate = document.getElementById('popup-date');
    const popupWriter = document.getElementById('popup-writer');
    const popupDetails = document.getElementById('popup-details');

    if (!updatePopup) return; // Exit if not on updates.html

    updateItems.forEach(item => {
        const updateId = item.getAttribute('data-update-id');
        const data = updateData[updateId];
        const itemImage = item.querySelector('img');

        if (data && data.image) {
            imageExists(`${data.image}`, (exists) => { // Path is relative to updates.html
                if (!exists) {
                    item.classList.add('no-image');
                    itemImage.insertAdjacentHTML('afterend', `<img src="assets/icon.png" alt="Rhythm Logo" class="fallback-logo">`);
                }
            });
        } else {
            item.classList.add('no-image');
            itemImage.insertAdjacentHTML('afterend', `<img src="assets/icon.png" alt="Rhythm Logo" class="fallback-logo">`);
        }

        item.addEventListener('click', () => {
            if (data) {
                if (data.image) {
                    imageExists(`${data.image}`, (exists) => { // Path is relative to updates.html
                        if (exists) {
                            popupImage.src = `${data.image}`;
                        } else {
                            popupImage.src = "assets/icon.png"; // Fallback to app logo
                        }
                    });
                } else {
                    popupImage.src = "assets/icon.png"; // Fallback to app logo
                }
                
                popupHeadline.textContent = data.headline;
                popupDate.textContent = data.date;
                popupWriter.textContent = data.writer;
                popupDetails.innerHTML = data.details;
                updatePopup.classList.add('active');
            }
        });
    });

    closePopupBtn.addEventListener('click', () => {
        updatePopup.classList.remove('active');
    });

    updatePopup.addEventListener('click', (e) => {
        if (e.target === updatePopup) {
            updatePopup.classList.remove('active');
        }
    });
}


// Smooth scrolling for anchor links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();

        const targetId = this.getAttribute('href');
        if (targetId === '#') {
            // For the logo, scroll to the absolute top
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        } else {
            const target = document.querySelector(targetId);
            if (target) {
                window.scrollTo({
                    top: target.offsetTop - 80,
                    behavior: 'smooth'
                });
            }
        }
    });
});


// Animation on scroll
function setupScrollAnimations() {
    const animateElements = document.querySelectorAll('.feature-card, .section-header, .dashboard-preview');

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.animation = 'fadeIn 0.6s forwards';
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.1 });

    animateElements.forEach(element => {
        element.style.opacity = '0';
        observer.observe(element);
    });
}

// Dark mode toggle
function setupDarkModeToggle() {
    const footerSection = document.querySelector('.footer-section:first-child');
    const darkModeToggle = document.createElement('div');
    darkModeToggle.className = 'dark-mode-toggle';
    darkModeToggle.innerHTML = `
        <label class="switch">
            <input type="checkbox" id="darkModeSwitch">
            <span class="slider"></span>
        </label>
        <span>Dark Mode</span>
    `;

    // Insert styles for the toggle
    const styleEl = document.createElement('style');
    styleEl.textContent = `
        .dark-mode-toggle {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-top: 20px;
        }
        .switch {
            position: relative;
            display: inline-block;
            width: 60px;
            height: 30px;
        }
        .switch input {
            opacity: 0;
            width: 0;
            height: 0;
        }
        .slider {
            position: absolute;
            cursor: pointer;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-color: #ccc;
            transition: .4s;
            border-radius: 34px;
        }
        .slider:before {
            position: absolute;
            content: "";
            height: 22px;
            width: 22px;
            left: 4px;
            bottom: 4px;
            background-color: white;
            transition: .4s;
            border-radius: 50%;
        }
        input:checked + .slider {
            background-color: var(--primary); /* Ensure dark mode toggle uses primary color */
        }
        input:checked + .slider:before {
            transform: translateX(30px);
        }
    `;
    document.head.appendChild(styleEl);

    footerSection.appendChild(darkModeToggle);

    const darkModeSwitch = document.getElementById('darkModeSwitch');

    // Function to apply theme
    function applyTheme(isDark) {
        if (isDark) {
            document.body.classList.add('dark-mode');
        } else {
            document.body.classList.remove('dark-mode');
        }
    }

    // Check for saved preference first
    const savedTheme = localStorage.getItem('darkMode');
    if (savedTheme === 'enabled') {
        applyTheme(true);
        darkModeSwitch.checked = true;
    } else if (savedTheme === 'disabled') {
        applyTheme(false);
        darkModeSwitch.checked = false;
    } else {
        // If no saved preference, check system preference
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        applyTheme(prefersDark);
        darkModeSwitch.checked = prefersDark;
    }

    // Listen for changes in system theme
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        // Only apply system theme if no explicit user preference is set
        if (localStorage.getItem('darkMode') === null) {
            applyTheme(e.matches);
            darkModeSwitch.checked = e.matches;
        }
    });

    // Dark mode toggle event listener
    darkModeSwitch.addEventListener('change', () => {
        if (darkModeSwitch.checked) {
            applyTheme(true);
            localStorage.setItem('darkMode', 'enabled');
        } else {
            applyTheme(false);
            localStorage.setItem('darkMode', 'disabled'); // Save 'disabled' to explicitly turn off dark mode
        }
    });
}

// Preloader
function setupPreloader() {
    const preloader = document.createElement('div');
    preloader.className = 'preloader';
    preloader.innerHTML = `
        <div class="spinner">
            <i class="fa-solid fa-arrows-rotate fa-spin"></i>
        </div>
    `;

    // Insert styles for the preloader
    const styleEl = document.createElement('style');
    styleEl.textContent = `
        .preloader {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background-color: var(--light);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 9999;
            transition: opacity 0.5s ease-out, visibility 0.5s ease-out;
        }
        .spinner {
            font-size: 80px;
            color: var(--primary);
            // animation: pulse 1.5s infinite;
        }
        @keyframes pulse {
            0% { transform: scale(0.9); opacity: 0.7; }
            50% { transform: scale(1.1); opacity: 1; }
            100% { transform: scale(0.9); opacity: 0.7; }
        }
        .preloader.hidden {
            opacity: 0;
            visibility: hidden;
        }
    `;
    document.head.appendChild(styleEl);

    document.body.prepend(preloader);

    window.addEventListener('load', () => {
        setTimeout(() => {
            preloader.classList.add('hidden');
        }, 300);
    });
}

// Initialize all functionality
document.addEventListener('DOMContentLoaded', () => {
    setupScrollAnimations();
    setupDarkModeToggle();
    setupPreloader();

    const currentPage = window.location.pathname.split('/').pop();

    if (currentPage === 'index.html' || currentPage === '') {
        setupNewsCarousel(); // Initialize news carousel functionality for index.html
        setupShowcaseCarousel(); // Initialize showcase carousel functionality for index.html
        setupScreenshotPopup(); // Initialize screenshot popup functionality for index.html
    } else if (currentPage === 'updates.html') {
        setupUpdatePopup(); // Initialize update popup functionality for updates.html
        setupUpdateViewToggle(); // Initialize update view toggle functionality for updates.html
    }
});

// Update View Toggle Functionality (for updates.html)
function setupUpdateViewToggle() {
    const listViewBtn = document.getElementById('listViewBtn');
    const gridViewBtn = document.getElementById('gridViewBtn');
    const updatesList = document.querySelector('.updates-list');

    if (!listViewBtn || !gridViewBtn || !updatesList) {
        return; // Exit if elements are not found
    }

    // Set default view to list view
    updatesList.classList.add('list-view');
    listViewBtn.classList.add('btn-primary', 'active');
    gridViewBtn.classList.remove('btn-primary', 'active');
    gridViewBtn.classList.add('btn-outline');

    listViewBtn.addEventListener('click', () => {
        updatesList.classList.remove('grid-view');
        updatesList.classList.add('list-view');
        listViewBtn.classList.add('btn-primary', 'active');
        listViewBtn.classList.remove('btn-outline');
        gridViewBtn.classList.remove('btn-primary', 'active');
        gridViewBtn.classList.add('btn-outline');
    });

    gridViewBtn.addEventListener('click', () => {
        updatesList.classList.remove('list-view');
        updatesList.classList.add('grid-view');
        gridViewBtn.classList.add('btn-primary', 'active');
        gridViewBtn.classList.remove('btn-outline');
        listViewBtn.classList.remove('btn-primary', 'active');
        listViewBtn.classList.add('btn-outline');
    });
}

// Smooth scrolling for all download buttons
document.querySelectorAll('.scroll-to-download').forEach(button => {
    button.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            window.scrollTo({
                top: target.offsetTop - 80, // Adjust for fixed header
                behavior: 'smooth'
            });
        }
    });
});

// Help Page Functionality - Material 3 Expressive
document.addEventListener('DOMContentLoaded', () => {
    // Help section collapsible functionality with improved animations
    const helpCategories = document.querySelectorAll('.help-category');

    helpCategories.forEach((category, categoryIndex) => {
        const header = category.querySelector('h2');
        const content = category.querySelectorAll('.help-item');

        // Set initial animation delays for staggered entry
        category.style.setProperty('--category-delay', `${categoryIndex * 0.1}s`);

        // Add click event to category headers
        header.addEventListener('click', () => {
            const isExpanded = category.classList.contains('expanded');

            // Close all categories first with smooth animation
            helpCategories.forEach(cat => {
                if (cat !== category) {
                    cat.classList.remove('expanded');
                    const items = cat.querySelectorAll('.help-item');
                    items.forEach((item, index) => {
                        item.style.setProperty('--item-delay', '0s');
                        item.classList.remove('item-visible');
                    });
                }
            });

            // Toggle current category
            if (!isExpanded) {
                category.classList.add('expanded');
                content.forEach((item, index) => {
                    // Set staggered animation delay
                    item.style.setProperty('--item-delay', `${index * 0.08}s`);
                    // Use requestAnimationFrame for smooth animation triggering
                    requestAnimationFrame(() => {
                        item.classList.add('item-visible');
                    });
                });
            } else {
                category.classList.remove('expanded');
                content.forEach(item => {
                    item.classList.remove('item-visible');
                });
            }
        });

        // Initially hide all help items with CSS classes
        content.forEach(item => {
            item.classList.remove('item-visible');
        });
    });

    // Help search functionality - Material 3 Express
    const searchInput = document.querySelector('.page-search');
    if (searchInput) {
        let searchTimeout;

        // Add search icon and loading state
        const searchWrapper = searchInput.parentElement;
        const searchIcon = document.createElement('i');
        searchIcon.className = 'fas fa-search';
        searchWrapper.appendChild(searchIcon);

        // Enhanced search functionality with debouncing
        searchInput.addEventListener('input', (e) => {
            clearTimeout(searchTimeout);
            const searchTerm = e.target.value.toLowerCase().trim();

            // Visual feedback
            searchIcon.className = searchTerm.length > 0 ? 'fas fa-times' : 'fas fa-search';

            searchTimeout = setTimeout(() => {
                const helpItems = document.querySelectorAll('.help-item');
                const helpCategories = document.querySelectorAll('.help-category');

                if (searchTerm.length === 0) {
                    // Smooth show all items
                    helpItems.forEach((item, index) => {
                        setTimeout(() => {
                            item.style.display = 'block';
                            item.style.opacity = '1';
                            item.style.transform = 'scale(1) translateY(0)';
                        }, index * 50);
                    });
                    helpCategories.forEach(category => {
                        category.style.display = 'block';
                    });
                    return;
                }

                // Search with animation
                let visibleCount = 0;
                helpItems.forEach((item, index) => {
                    const text = item.textContent.toLowerCase();
                    const isVisible = text.includes(searchTerm);

                    if (isVisible) {
                        visibleCount++;
                        setTimeout(() => {
                            item.style.display = 'block';
                            item.style.opacity = '1';
                            item.style.transform = 'scale(1) translateY(0)';
                        }, index * 30);
                    } else {
                        item.style.opacity = '0';
                        item.style.transform = 'scale(0.95) translateY(-10px)';
                        setTimeout(() => {
                            item.style.display = 'none';
                        }, 200);
                    }
                });

                // Hide categories that have no visible items
                helpCategories.forEach(category => {
                    const visibleItems = category.querySelectorAll('.help-item[style*="display: block"]');
                    if (visibleItems.length > 0) {
                        category.style.display = 'block';
                        category.style.opacity = '1';
                        category.style.transform = 'translateY(0)';
                    } else {
                        category.style.opacity = '0';
                        category.style.transform = 'translateY(-20px)';
                        setTimeout(() => {
                            category.style.display = 'none';
                        }, 300);
                    }
                });

                // Show "no results" message if needed
                let noResultsMsg = document.querySelector('.no-results');
                if (visibleCount === 0 && searchTerm.length > 0) {
                    if (!noResultsMsg) {
                        noResultsMsg = document.createElement('div');
                        noResultsMsg.className = 'no-results';
                        noResultsMsg.style.cssText = `
                            text-align: center;
                            padding: 40px;
                            color: var(--on-surface-variant);
                            font-size: 18px;
                            animation: fadeIn 0.3s ease;
                        `;
                        noResultsMsg.innerHTML = `
                            <i class="fas fa-search" style="font-size: 48px; color: var(--surface-variant); margin-bottom: 16px; display: block;"></i>
                            No results found for "${searchTerm}"
                        `;
                        document.querySelector('.help-content').appendChild(noResultsMsg);
                    }
                } else if (noResultsMsg) {
                    noResultsMsg.remove();
                }
            }, 300);
        });

        // Clear search on icon click
        searchIcon.addEventListener('click', () => {
            if (searchInput.value.length > 0) {
                searchInput.value = '';
                searchInput.dispatchEvent(new Event('input'));
            }
        });
    }

    // Add smooth scrolling to help category links
    const helpLinks = document.querySelectorAll('a[href^="#"]');
    helpLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            const targetId = link.getAttribute('href');
            const targetElement = document.querySelector(targetId);

            if (targetElement) {
                e.preventDefault();

                // Use scrollIntoView with block: 'start' and adjust for header
                const header = document.querySelector('header');
                const headerHeight = header ? header.offsetHeight : 80;

                // Temporarily adjust scroll-margin-top for smooth scrolling
                targetElement.style.scrollMarginTop = `${headerHeight + 20}px`;

                targetElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });

                // Reset scroll-margin-top after scrolling
                setTimeout(() => {
                    targetElement.style.scrollMarginTop = '';
                }, 1000);
            }
        });
    });

    // Add animation on scroll for help items
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
            }
        });
    }, observerOptions);

    // Observe help categories
    document.querySelectorAll('.help-category').forEach(category => {
        category.style.opacity = '0';
        category.style.transform = 'translateY(30px)';
        category.style.transition = 'all 0.6s ease';
        observer.observe(category);
    });
});

// Back to Top Button
document.addEventListener('DOMContentLoaded', () => {
    // Create back to top button
    const backToTopBtn = document.createElement('button');
    backToTopBtn.className = 'back-to-top';
    backToTopBtn.innerHTML = '<i class="fas fa-arrow-up"></i>';
    backToTopBtn.setAttribute('aria-label', 'Back to top');
    document.body.appendChild(backToTopBtn);

    // Show/hide back to top button
    window.addEventListener('scroll', () => {
        if (window.scrollY > 300) {
            backToTopBtn.classList.add('show');
        } else {
            backToTopBtn.classList.remove('show');
        }
    });

    // Scroll to top when clicked
    backToTopBtn.addEventListener('click', () => {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });

    // Reading progress indicator for help page
    if (document.querySelector('.help-section')) {
        const progressBar = document.createElement('div');
        progressBar.className = 'reading-progress';
        document.body.appendChild(progressBar);

        window.addEventListener('scroll', () => {
            const scrollTop = window.scrollY;
            const docHeight = document.documentElement.scrollHeight - window.innerHeight;
            const scrollPercent = (scrollTop / docHeight) * 100;
            progressBar.style.width = scrollPercent + '%';
        });
    }
});

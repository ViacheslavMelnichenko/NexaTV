# Background artwork

Generated with the built-in imagegen tool on 2026-10-21, using the user's supplied landscape montage as a visual reference. This is an illustrative river-city scene, not a geographical representation of Wrocław.

All 16 delivered WebP assets are in `app/src/main/assets/backgrounds/`. Native generated dimensions are retained (1672 × 941, approximately 16:9); WebP quality 82. No runtime image downloads or generation.

## Base prompt

A single full-bleed 16:9 photorealistic Android TV landscape: peaceful elevated garden viewpoint overlooking a broad river, low bridge, islands and distant European city skyline. Leafy branches at far left/top-left, low rocks and vegetation at bottom, open sky above, warm natural afternoon daylight, muted cinematic colors, atmospheric haze and restrained central contrast. Sun out of frame. No people, text, UI, collage or watermark.

## Clear time-of-day edits

Preserve exact camera, crop, bridge piers, skyline, islands, tree branches and rocks. Change only lighting, sky and appropriate lights/reflections.

- Dawn: pale rose/lavender sky, peach horizon, morning river mist, cool foreground.
- Sunset: muted apricot sky, gentle peach clouds, warm light from right, city lights beginning.
- Night: calm deep blue, faint stars, small upper-right moon, warm windows and street lights reflected in the river.

## Weather edits (one edit per asset)

### day_rain

Use case: lighting-weather. Edit target: attached day river landscape. Asset type: single 16:9 Android TV wallpaper. Change ONLY weather to: Rainy weather. Soft dark rain clouds and distant rain haze, wet reflective rocks and leaves, calm muted slate colors. No visible sun, moon or stars. NO distinct falling rain streaks or drops anywhere, because animated rain will be added by the app. Preserve the day time of day and its appropriate brightness and color temperature (night must stay dark blue with city lights; dawn muted pale rose; sunset muted warm cloudy horizon; day cool daylight). CRITICAL preserve the EXACT camera, crop, skyline buildings, bridge piers, islands, every tree branch and rock position from the input image. Do not redesign or shift any terrain or building. Keep calm photorealistic cinematic mood, restrained contrast. No text, UI, collage, borders or watermarks.

### sunset_cloudy

Use case: lighting-weather. Edit target: attached sunset river landscape. Asset type: single 16:9 Android TV wallpaper. Change ONLY weather to: Overcast cloudy weather. Thick softly layered cloud cover, diffuse subdued light, slightly hazy skyline. No visible sun, moon or stars. No precipitation. Preserve the sunset time of day and its appropriate brightness and color temperature (night must stay dark blue with city lights; dawn muted pale rose; sunset muted warm cloudy horizon; day cool daylight). CRITICAL preserve the EXACT camera, crop, skyline buildings, bridge piers, islands, every tree branch and rock position from the input image. Do not redesign or shift any terrain or building. Keep calm photorealistic cinematic mood, restrained contrast. No text, UI, collage, borders or watermarks.

### day_snow

Use case: lighting-weather. Edit target: attached day river landscape. Asset type: single 16:9 Android TV wallpaper. Change ONLY weather to: Snowy weather. Cloudy sky with soft diffuse light, a light dusting of snow on the existing rocks, bridge edges and vegetation, soft distant snow haze. Preserve tree branch geometry. No visible sun, moon or stars. NO distinct falling snowflakes because animated snow will be added by the app. Preserve the day time of day and its appropriate brightness and color temperature (night must stay dark blue with city lights; dawn muted pale rose; sunset muted warm cloudy horizon; day cool daylight). CRITICAL preserve the EXACT camera, crop, skyline buildings, bridge piers, islands, every tree branch and rock position from the input image. Do not redesign or shift any terrain or building. Keep calm photorealistic cinematic mood, restrained contrast. No text, UI, collage, borders or watermarks.

### sunset_rain

Use case: lighting-weather. Edit target: attached sunset river landscape. Asset type: single 16:9 Android TV wallpaper. Change ONLY weather to: Rainy weather. Soft dark rain clouds and distant rain haze, wet reflective rocks and leaves, calm muted slate colors. No visible sun, moon or stars. NO distinct falling rain streaks or drops anywhere, because animated rain will be added by the app. Preserve the sunset time of day and its appropriate brightness and color temperature (night must stay dark blue with city lights; dawn muted pale rose; sunset muted warm cloudy horizon; day cool daylight). CRITICAL preserve the EXACT camera, crop, skyline buildings, bridge piers, islands, every tree branch and rock position from the input image. Do not redesign or shift any terrain or building. Keep calm photorealistic cinematic mood, restrained contrast. No text, UI, collage, borders or watermarks.

### sunset_snow

Use case: lighting-weather. Edit target: attached sunset river landscape. Asset type: single 16:9 Android TV wallpaper. Change ONLY weather to: Snowy weather. Cloudy sky with soft diffuse light, a light dusting of snow on the existing rocks, bridge edges and vegetation, soft distant snow haze. Preserve tree branch geometry. No visible sun, moon or stars. NO distinct falling snowflakes because animated snow will be added by the app. Preserve the sunset time of day and its appropriate brightness and color temperature (night must stay dark blue with city lights; dawn muted pale rose; sunset muted warm cloudy horizon; day cool daylight). CRITICAL preserve the EXACT camera, crop, skyline buildings, bridge piers, islands, every tree branch and rock position from the input image. Do not redesign or shift any terrain or building. Keep calm photorealistic cinematic mood, restrained contrast. No text, UI, collage, borders or watermarks.

### day_cloudy

Use case: lighting-weather. Edit target: attached day river landscape. Asset type: single 16:9 Android TV wallpaper. Change ONLY weather to: Overcast cloudy weather. Thick softly layered cloud cover, diffuse subdued light, slightly hazy skyline. No visible sun, moon or stars. No precipitation. Preserve the day time of day and its appropriate brightness and color temperature (night must stay dark blue with city lights; dawn muted pale rose; sunset muted warm cloudy horizon; day cool daylight). CRITICAL preserve the EXACT camera, crop, skyline buildings, bridge piers, islands, every tree branch and rock position from the input image. Do not redesign or shift any terrain or building. Keep calm photorealistic cinematic mood, restrained contrast. No text, UI, collage, borders or watermarks.

### night_rain

Use case: lighting-weather. Edit target: attached night river landscape. Asset type: single 16:9 Android TV wallpaper. Change ONLY weather to: Rainy weather. Soft dark rain clouds and distant rain haze, wet reflective rocks and leaves, calm muted slate colors. No visible sun, moon or stars. NO distinct falling rain streaks or drops anywhere, because animated rain will be added by the app. Preserve the night time of day and its appropriate brightness and color temperature (night must stay dark blue with city lights; dawn muted pale rose; sunset muted warm cloudy horizon; day cool daylight). CRITICAL preserve the EXACT camera, crop, skyline buildings, bridge piers, islands, every tree branch and rock position from the input image. Do not redesign or shift any terrain or building. Keep calm photorealistic cinematic mood, restrained contrast. No text, UI, collage, borders or watermarks.

### night_cloudy

Use case: lighting-weather. Edit target: attached night river landscape. Asset type: single 16:9 Android TV wallpaper. Change ONLY weather to: Overcast cloudy weather. Thick softly layered cloud cover, diffuse subdued light, slightly hazy skyline. No visible sun, moon or stars. No precipitation. Preserve the night time of day and its appropriate brightness and color temperature (night must stay dark blue with city lights; dawn muted pale rose; sunset muted warm cloudy horizon; day cool daylight). CRITICAL preserve the EXACT camera, crop, skyline buildings, bridge piers, islands, every tree branch and rock position from the input image. Do not redesign or shift any terrain or building. Keep calm photorealistic cinematic mood, restrained contrast. No text, UI, collage, borders or watermarks.

### night_snow

Use case: lighting-weather. Edit target: attached night river landscape. Asset type: single 16:9 Android TV wallpaper. Change ONLY weather to: Snowy weather. Cloudy sky with soft diffuse light, a light dusting of snow on the existing rocks, bridge edges and vegetation, soft distant snow haze. Preserve tree branch geometry. No visible sun, moon or stars. NO distinct falling snowflakes because animated snow will be added by the app. Preserve the night time of day and its appropriate brightness and color temperature (night must stay dark blue with city lights; dawn muted pale rose; sunset muted warm cloudy horizon; day cool daylight). CRITICAL preserve the EXACT camera, crop, skyline buildings, bridge piers, islands, every tree branch and rock position from the input image. Do not redesign or shift any terrain or building. Keep calm photorealistic cinematic mood, restrained contrast. No text, UI, collage, borders or watermarks.

### dawn_cloudy

Use case: lighting-weather. Edit target: attached dawn river landscape. Asset type: single 16:9 Android TV wallpaper. Change ONLY weather to: Overcast cloudy weather. Thick softly layered cloud cover, diffuse subdued light, slightly hazy skyline. No visible sun, moon or stars. No precipitation. Preserve the dawn time of day and its appropriate brightness and color temperature (night must stay dark blue with city lights; dawn muted pale rose; sunset muted warm cloudy horizon; day cool daylight). CRITICAL preserve the EXACT camera, crop, skyline buildings, bridge piers, islands, every tree branch and rock position from the input image. Do not redesign or shift any terrain or building. Keep calm photorealistic cinematic mood, restrained contrast. No text, UI, collage, borders or watermarks.

### dawn_snow

Use case: lighting-weather. Edit target: attached dawn river landscape. Asset type: single 16:9 Android TV wallpaper. Change ONLY weather to: Snowy weather. Cloudy sky with soft diffuse light, a light dusting of snow on the existing rocks, bridge edges and vegetation, soft distant snow haze. Preserve tree branch geometry. No visible sun, moon or stars. NO distinct falling snowflakes because animated snow will be added by the app. Preserve the dawn time of day and its appropriate brightness and color temperature (night must stay dark blue with city lights; dawn muted pale rose; sunset muted warm cloudy horizon; day cool daylight). CRITICAL preserve the EXACT camera, crop, skyline buildings, bridge piers, islands, every tree branch and rock position from the input image. Do not redesign or shift any terrain or building. Keep calm photorealistic cinematic mood, restrained contrast. No text, UI, collage, borders or watermarks.

### dawn_rain

Use case: lighting-weather. Edit target: attached dawn river landscape. Asset type: single 16:9 Android TV wallpaper. Change ONLY weather to: Rainy weather. Soft dark rain clouds and distant rain haze, wet reflective rocks and leaves, calm muted slate colors. No visible sun, moon or stars. NO distinct falling rain streaks or drops anywhere, because animated rain will be added by the app. Preserve the dawn time of day and its appropriate brightness and color temperature (night must stay dark blue with city lights; dawn muted pale rose; sunset muted warm cloudy horizon; day cool daylight). CRITICAL preserve the EXACT camera, crop, skyline buildings, bridge piers, islands, every tree branch and rock position from the input image. Do not redesign or shift any terrain or building. Keep calm photorealistic cinematic mood, restrained contrast. No text, UI, collage, borders or watermarks.



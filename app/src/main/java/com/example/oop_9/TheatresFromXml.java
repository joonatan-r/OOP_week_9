package com.example.oop_9;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

class TheatresFromXml {
    private ArrayList<TheatreArea> theatreAreas = new ArrayList<TheatreArea>();
    // "High level" means that they contain the other areas
    private ArrayList<TheatreArea> highLvlAreas = new ArrayList<TheatreArea>();
    private HashMap<String,TheatreArea> theatreMap = new HashMap<String, TheatreArea>();

    TheatresFromXml() {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            String urlString = "https://www.finnkino.fi/xml/TheatreAreas/";
            Document doc = db.parse(urlString);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getDocumentElement().getElementsByTagName("TheatreArea");

            for (int i = 0; i < nList.getLength(); i++) {
                Node node = nList.item(i);
                String id;
                String location;
                String name;
                String[] info;

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) node;
                    id = el.getElementsByTagName("ID").item(0).getTextContent();
                    name = el.getElementsByTagName("Name").item(0).getTextContent();
                    info = name.split(": ");
                    location = info[0];

                    // theatre areas that don't have name contain other areas,
                    // so no need to include them in unique theatres
                    if (info.length > 1) {
                        name = info[1];
                        theatreAreas.add(new TheatreArea(id, location, name));

                        // add those theaters to high lvl that only have one theatre in area.
                        // Helsinki, Espoo, Vantaa are special case that share one high lvl area.
                        if (!alreadyHave(location) && !location.equals("Espoo")
                            && !location.equals("Helsinki") && !location.equals("Vantaa")) {
                            highLvlAreas.add(new TheatreArea(id, location, null));
                        }
                    } else if (!id.equals("1029") && !location.equals("Espoo")
                               && !location.equals("Helsinki") && !location.equals("Vantaa")) {
                        // 1029 is highest lvl but doesn't contain all, so ignore it.
                        // These are the areas that don't have a specific name, so they are always
                        // high lvl
                        highLvlAreas.add(new TheatreArea(id, location, null));
                    }
                }
            }

            for (TheatreArea t : theatreAreas) {
                theatreMap.put(t.location + ", " + t.name, t);
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    private boolean alreadyHave(String location) {
        for (TheatreArea t : highLvlAreas) {
            if (t.location.equals(location)) {
                return true;
            }
        }
        return false;
    }

    String getShows(String id, String dateString, String intervalStart, String intervalEnd, String movie) throws Exception {
        String[] intervalStartStrings, intervalEndStrings;
        int intervalStartHour = 0, intervalStartMinute = 0, intervalEndHour = 0, intervalEndMinute = 0;

        if (!intervalStart.equals("") && !intervalEnd.equals("")) {
            intervalStartStrings = intervalStart.split(":");
            intervalStartHour = Integer.parseInt(intervalStartStrings[0]);
            intervalStartMinute = Integer.parseInt(intervalStartStrings[1]);
            intervalEndStrings = intervalEnd.split(":");
            intervalEndHour = Integer.parseInt(intervalEndStrings[0]);
            intervalEndMinute = Integer.parseInt(intervalEndStrings[1]);

            if (intervalStartHour > 24 || intervalStartHour < 0 || intervalStartMinute > 59
                || intervalStartMinute < 0 || intervalEndHour > 24 ||intervalEndHour < 0
                || intervalEndMinute > 59 || intervalEndMinute < 0
                || intervalStartStrings.length > 2 || intervalEndStrings.length > 2) {
                throw new Exception();
            }
        }

        ArrayList<String> urlList = new ArrayList<String>();
        Calendar cal = Calendar.getInstance();
        DateFormat dfFrom = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        DateFormat dfDay = new SimpleDateFormat("dd.MM.yyyy");
        DateFormat dfHours = new SimpleDateFormat("HH:mm");
        StringBuilder sb = new StringBuilder();

        if (dateString.equals("")) {
            dateString = dfDay.format(cal.getTime());
        }

        if (!movie.equals("")) sb.append(movie).append("\n\n");

        if (id.equals("ALL")) {
            for (TheatreArea t : highLvlAreas) {
                urlList.add("https://www.finnkino.fi/xml/Schedule/?area=" + t.id + "&dt=" + dateString);
            }
        } else {
            urlList.add("https://www.finnkino.fi/xml/Schedule/?area=" + id + "&dt=" + dateString);
        }

        for (String urlString : urlList) {
            try {
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = db.parse(urlString);
                doc.getDocumentElement().normalize();
                Node showsNode = doc.getDocumentElement().getElementsByTagName("Shows").item(0);
                NodeList nList = showsNode.getChildNodes();

                for (int i = 0; i < nList.getLength(); i++) {
                    Date startDate, intervalStartDate, intervalEndDate;
                    Node node = nList.item(i);
                    String title, place, startString, startDay, startHours, theatre;

                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element el = (Element) node;
                        title = el.getElementsByTagName("Title").item(0).getTextContent();
                        place = el.getElementsByTagName("TheatreAuditorium").item(0).getTextContent();

                        startString = el.getElementsByTagName("dttmShowStart").item(0).getTextContent();
                        startDate = dfFrom.parse(startString);

                        if (!movie.equals("")) {
                            if (!movie.equals(title)) continue;
                        }

                        if (!intervalStart.equals("") && !intervalEnd.equals("")) {
                            cal.setTime(startDate);
                            cal.set(Calendar.HOUR_OF_DAY, intervalStartHour);
                            cal.set(Calendar.MINUTE, intervalStartMinute);
                            intervalStartDate = cal.getTime();
                            cal.set(Calendar.HOUR_OF_DAY, intervalEndHour);
                            cal.set(Calendar.MINUTE, intervalEndMinute);
                            intervalEndDate = cal.getTime();

                            if (startDate.before(intervalStartDate) || startDate.after(intervalEndDate)) {
                                continue;
                            }
                        }

                        startDay = dfDay.format(startDate);
                        startHours = dfHours.format(startDate);
                        theatre = el.getElementsByTagName("Theatre").item(0).getTextContent();

                        if (id.equals("ALL")) sb.append(theatre).append("\n");

                        if (movie.equals("")) {
                            sb.append(String.format("%10s %5s %8s %-10s\n\n", startDay, startHours, place, title));
                        } else {
                            sb.append(String.format("%10s %5s %8s\n\n", startDay, startHours, place));
                        }
                    }
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }

    ArrayList<TheatreArea> getTheatreAreas() {
        return theatreAreas;
    }

    HashMap<String,TheatreArea> getTheatreMap() {
        return theatreMap;
    }
}

<h1>Search engine</h1>
<h3>Crawl sites and index all of its pages for relevant search of pages for the specified request</h3>
<hr>
<h4>Now in development &#8987;</h4>

- [x] Implemented site parsing with search for links
- [x] Multi-threaded adding to the database
- [ ] ...
<hr>


<h2>&#x2754; How it works</h2>
<ul style="font-size:20px">
<li> In the configuration file, before launching the application, 
the addresses of sites that the engine should search are specified.</li>

<li> The search engine must independently crawl all pages
given sites and index them (create a so-called index)
so that you can then find the most relevant pages for any
search query.</li>
<li> The user sends a request via the engine API. A request is a set
words by which you need to find website pages.</li>
<li> The query is transformed in a certain way into a list of words,
converted to basic form. For example, for nouns - nominative case, singular.</li>
<li> The index searches for pages where all these words appear.</li>
<li> Search results are ranked, sorted and given to the user.</li>
</ul>


<h2>&#x1F50D; In details</h2>
<div style="font-size:20px">

<h3>App structure (currently)</h3>

![database](https://github.com/Nikomankov/Search_engine/blob/master/readme_assets/AppScheme.png)


<h3>Data base structure</h3>

![database](https://github.com/Nikomankov/Search_engine/blob/master/readme_assets/database.png)

<h3>Stages of application operation</h3>

<i><u>(this block will be added to as development progresses)</u></i>
<ul>
<li>

Sites for indexing are set in the `application.yaml` file. It looks like this

```yaml
indexing-settings:
  sites:
    - url: https://www.lenta.ru
      name: Лента.ру
    - url: https://www.skillbox.ru
      name: Skillbox
    - url: https://www.playback.ru
      name: PlayBack.Ru
```
</li>

<li>Page crawling occurs in multi-threaded mode. For each page, an instance of 
ForkJoinTask is created, in which links to other pages of this site are searched. Information from 
each page is transferred to a database with indexing along the path of that page. Found links are 
saved into a common set for this site.</li>

<br>

<li>The page content is cleared of html tags. Lemmas are created for each word 
(with the exception of auxiliary parts of speech).
<br>

>Lemma - in morphology and lexicography, a lemma (pl.: lemmas or lemmata) is the canonical form, dictionary form, or citation form of a set of word forms.

The set of lemmas and their number are stored in the database. The "lemma"
table stores the lemma itself and the "frequency" value corresponding to the number of pages where
this lemma is present. A bunch of lemmas and pages are stored in the "index" table, indicating the
number of lemmas on the page.</li>

<br>

<li>When searching, the user enters a query. The search query is divided into individual words and 
forms a list of lemmas. During the search process, lemmas that appear on too many sites are 
excluded. We carry out the search starting with the most rarely encountered lemma, and with each 
iteration with a new lemma we shorten the list of pages.
For each page, it calculates absolute relevance as the sum of all “rank” lemmas found on the page.

Let's look at the example of the search query "Running horses"

| Page | Rank "Horse" | Rank "Run" | Absolute relevance | Relative relevance |
|------|--------------|------------|--------------------|--------------------|
| 1    | 4,2          | 3,1        | 7,3                | 0,7                |
| 2    | 1,0          | 1,5        | 2,5                | 0,24               |
| 3    | 9,9          | 0,4        | 10,3               | 1                  |

    Rabs = 4,2 + 3,1 = 7,3
    Rrel = 7,3 / 10,3 = 0,7087

Pages are sorted in descending order of relevance and displayed as a list of objects with the
following fields:
<ul>
<li>url — path to the page like /path/to/page/6784;</li>
<li>title — page title;</li>
<li>snippet — a text fragment in which matches were found;</li>
<li>relevance — page relevance (see formula above
calculation).</li>
</ul>
</li>
</ul>

</div>

<h2>&#x1F527; Tech stack</h2>
<ul style="font-size:17px">
<li>Java 19</li>
<li>Spring (boot, web, jpa)</li>
<li>Hibernate</li>
<li>MySQL</li>
<li>Jsoup</li>
<li>JS</li>
</ul>

<h2>&#129490; Author</h2>
<div style="font-size:20px; margin-left: 20px;">

<h3>Mankov Nikita</h3>

[Telegram](https://t.me/Nikomankov)

[LeetCode](https://leetcode.com/nikomankov/)

kislobaa@gmail.com

</div>

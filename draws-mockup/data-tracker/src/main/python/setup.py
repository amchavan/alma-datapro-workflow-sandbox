from setuptools import setup

dependencies = [
    "draws-mock-icd",
    "drawsmb"
]

setup(name='draws-mock-data-tracker',
      version='0.1',
      description='DRAWS Mock DataTracker',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['draws', 'draws/mock', 'draws/mock/archive'],
      install_requires=dependencies,
      zip_safe=False)
